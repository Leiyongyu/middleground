package com.asinking.com.openapi.service.impl;

import com.asinking.com.openapi.common.response.PageResult;
import com.asinking.com.openapi.dto.response.InventoryOverviewItem;
import com.asinking.com.openapi.entity.DailyPriceTrackingEntity;
import com.asinking.com.openapi.service.DailyPriceTrackingService;
import com.asinking.com.openapi.service.InventoryOverviewService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 每日跟价服务实现：从运营总览（补货页）数据映射为每日跟价字段，运行时过滤分页。
 */
@Service
public class DailyPriceTrackingServiceImpl implements DailyPriceTrackingService {

    private final InventoryOverviewService overviewService;

    public DailyPriceTrackingServiceImpl(InventoryOverviewService overviewService) {
        this.overviewService = overviewService;
    }

    @Override
    public PageResult<DailyPriceTrackingEntity> page(long page, long size,
                                                      String site, String sku, String brand, String operator) {
        // 1. 获取补货页全量数据
        List<InventoryOverviewItem> overviewItems = overviewService.buildOverview();
        if (overviewItems == null) overviewItems = Collections.emptyList();

        // 2. 映射为每日跟价实体
        List<DailyPriceTrackingEntity> allRows = overviewItems.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());

        // 3. 内存过滤（与补货页的 filterOverview 逻辑一致）
        List<DailyPriceTrackingEntity> filtered = allRows.stream()
                .filter(e -> match(site, e.getSite()))
                .filter(e -> matchContains(sku, e.getSku()))
                .filter(e -> matchContains(brand, e.getBrand()))
                .filter(e -> matchContains(operator, e.getOperator()))
                .collect(Collectors.toList());

        // 4. 内存分页
        long p = page <= 0 ? 1 : page;
        long s = size <= 0 ? 20 : Math.min(size, 200);
        long total = filtered.size();
        int from = (int) ((p - 1) * s);
        int to = (int) Math.min(from + s, total);
        List<DailyPriceTrackingEntity> pageRecords = from < total
                ? filtered.subList(from, to)
                : Collections.emptyList();

        return new PageResult<>(total, p, s, pageRecords);
    }

    /** InventoryOverviewItem → DailyPriceTrackingEntity */
    private DailyPriceTrackingEntity toEntity(InventoryOverviewItem item) {
        DailyPriceTrackingEntity e = new DailyPriceTrackingEntity();
        // ===== 直接对应的字段 =====
        e.setSite(item.getWarehouseNames());                // 站点
        e.setSku(item.getSku());                            // SKU
        e.setLast7DaysSales(item.getLast7DaysSales());      // 近7天销量
        e.setLast30DaysSales(item.getLast30DaysSales());    // 近30天销量
        e.setLast90DaysSales(item.getLast90DaysSales());    // 近90天销量
        e.setMaxMonthlySales(item.getMaxMonthlySales());    // 历史最大月销

        // ===== 推导映射 =====
        e.setOverseasWarehouseStock(item.getOverseasSellable());             // 海外仓库存 = 海外可售
        e.setEstimatedReplenish(item.getPurchaseQuantity() != null           // 预估补货量 = 采购数量
                ? item.getPurchaseQuantity().intValue() : null);
        e.setStockSalesRatio(item.getOverseasInStockRatio());                // 库销比
        e.setBrand(extractBrand(item.getSku()));                              // 品牌 = SKU 前缀
        e.setOperator(item.getOwner());                                      // 操作员 = 负责人

        // ===== 暂不可从补货数据获取的字段 =====
        e.setSkuLevel(item.getSkuLevel());
        e.setOurLowestPrice(null);
        e.setTrackingPrice(null);
        e.setTrackingProfitMargin(null);
        e.setFloorPrice(null);
        e.setReturnRate(null);
        e.setLast3DaysSales(null);
        e.setEbayFrontpageUrl(null);
        e.setFrontpageSoldUrl(null);
        e.setOverseasWarehouseAge(null);
        e.setRemark(null);

        return e;
    }

    /** 匹配：空值全部通过；逗号分隔时为多选匹配，否则精确匹配 */
    private boolean match(String filter, String actual) {
        if (filter == null || filter.trim().isEmpty()) return true;
        if (filter.contains(",")) {
            return Arrays.stream(filter.split(","))
                    .map(String::trim)
                    .anyMatch(s -> s.equals(actual));
        }
        return filter.trim().equals(actual);
    }

    /** 从 SKU 截取品牌前缀（如 MCD-20018 → MCD） */
    private String extractBrand(String sku) {
        if (sku == null || sku.isEmpty()) return "";
        int i = sku.indexOf('-');
        return i > 0 ? sku.substring(0, i).toUpperCase() : sku.toUpperCase();
    }

    /** 模糊匹配 */
    private boolean matchContains(String keyword, String actual) {
        if (keyword == null || keyword.trim().isEmpty()) return true;
        if (actual == null) return false;
        return actual.toLowerCase().contains(keyword.trim().toLowerCase());
    }
}
