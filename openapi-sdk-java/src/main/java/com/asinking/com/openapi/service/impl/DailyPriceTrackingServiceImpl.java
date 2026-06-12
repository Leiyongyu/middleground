package com.asinking.com.openapi.service.impl;

import com.asinking.com.openapi.common.response.PageResult;
import com.asinking.com.openapi.service.EbayLinkTemplateService;

import com.asinking.com.openapi.config.LingxingProperties;
import com.asinking.com.openapi.dto.response.DailyPriceTrackingItem;
import com.asinking.com.openapi.entity.*;
import com.asinking.com.openapi.mapper.mp.*;
import com.asinking.com.openapi.service.*;
import com.asinking.com.openapi.utils.InventoryUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

/**
 * 每日跟价服务实现：独立于补货页的数据查询与计算。
 * 直接从各数据源查询，使用 {@link InventoryUtils} 共享工具方法。
 * 预估补货量直接从补货页的采购数量匹配（按站点+去除PC前缀的SKU）。
 */
@Service
public class DailyPriceTrackingServiceImpl implements DailyPriceTrackingService {

    private static final Logger LOG = LoggerFactory.getLogger(DailyPriceTrackingServiceImpl.class);
    private final InventoryComputeEngine engine;
    private final LingxingProperties lingxingProperties;
    private final WarehouseService warehouseService;
    private final WarehouseInventoryDetailService inventoryService;
    private final EbaySalesMapper ebaySalesMapper;
    private final GoodcangGrnListMapper grnListMapper;
    private final GoodcangGrnDetailMapper grnDetailMapper;
    private final GoodcangWarehouseMapper gcWarehouseMapper;
    private final InventoryOverviewMapper overviewMapper;
    private final EbayProductDedupService dedupService;
    private final EbayLinkTemplateService linkTemplateService;
    public DailyPriceTrackingServiceImpl(LingxingProperties lingxingProperties,
                                         WarehouseService warehouseService,
                                         WarehouseInventoryDetailService inventoryService,
                                         EbaySalesMapper ebaySalesMapper,
                                         GoodcangGrnListMapper grnListMapper,
                                         GoodcangGrnDetailMapper grnDetailMapper,
                                         GoodcangWarehouseMapper gcWarehouseMapper,
                                         InventoryOverviewMapper overviewMapper,
                                         EbayProductDedupService dedupService,
                                         EbayLinkTemplateService linkTemplateService,
                                         InventoryComputeEngine engine) {
        this.lingxingProperties = lingxingProperties;
        this.warehouseService = warehouseService;
        this.inventoryService = inventoryService;
        this.ebaySalesMapper = ebaySalesMapper;
        this.grnListMapper = grnListMapper;
        this.grnDetailMapper = grnDetailMapper;
        this.gcWarehouseMapper = gcWarehouseMapper;
        this.overviewMapper = overviewMapper;
        this.dedupService = dedupService;
        this.linkTemplateService = linkTemplateService;
        this.engine = engine;
    }

    @Override
    public PageResult<DailyPriceTrackingItem> page(long page, long size,
                                                   String site, String sku, String brand, String operator,
                                                   String sortField, String sortOrder) {
        // 1. 优先从缓存表读取，表为空则实时计算
        Long tableCount = overviewMapper.selectCount(null);
        List<DailyPriceTrackingItem> allRows;
        if (tableCount != null && tableCount > 0) {
            allRows = overviewMapper.selectList(null).stream().map(this::entityToDto).collect(Collectors.toList());
        } else {
            allRows = computeDailyPriceTracking();
        }

        // 1b. 从 ebay_product_dedup 补充实时跟价字段
        fillTrackingFromDedup(allRows);

        // 2. 内存筛选
        List<DailyPriceTrackingItem> filtered = allRows.stream()
                .filter(e -> matchMulti(site, e.getSite()))
                .filter(e -> matchContains(sku, e.getSku()))
                .filter(e -> matchContains(brand, e.getBrand()))
                .filter(e -> matchContains(operator, e.getOperator()))
                .collect(Collectors.toList());

        // 3. 排序
        if (StringUtils.hasText(sortField) && StringUtils.hasText(sortOrder)) {
            boolean asc = "asc".equalsIgnoreCase(sortOrder.trim());
            Comparator<DailyPriceTrackingItem> cmp = getSortComparator(sortField.trim(), asc);
            if (cmp != null) filtered.sort(cmp);
        }

        // 4. 内存分页
        long p = page <= 0 ? 1 : page;
        long s = size <= 0 ? 20 : Math.min(size, 200);
        long total = filtered.size();
        int from = (int) ((p - 1) * s);
        int to = (int) Math.min(from + s, total);
        List<DailyPriceTrackingItem> pageRecords = from < total
                ? filtered.subList(from, to)
                : Collections.emptyList();

        return new PageResult<>(total, p, s, pageRecords);
    }

    private Comparator<DailyPriceTrackingItem> getSortComparator(String field, boolean asc) {
        java.util.function.Function<DailyPriceTrackingItem, Comparable> getter;
        switch (field) {
            case "site": getter = DailyPriceTrackingItem::getSite; break;
            case "sku": getter = DailyPriceTrackingItem::getSku; break;
            case "skuLevel": getter = DailyPriceTrackingItem::getSkuLevel; break;
            case "ourLowestPrice": getter = i -> i.getOurLowestPrice(); break;
            case "trackingPrice": getter = i -> i.getTrackingPrice(); break;
            case "trackingProfitMargin": getter = i -> i.getTrackingProfitMargin(); break;
            case "floorPrice": getter = i -> i.getFloorPrice(); break;
            case "returnRate": getter = i -> i.getReturnRate(); break;
            case "last3DaysSales": getter = i -> i.getLast3DaysSales(); break;
            case "last7DaysSales": getter = i -> i.getLast7DaysSales(); break;
            case "last30DaysSales": getter = i -> i.getLast30DaysSales(); break;
            case "last90DaysSales": getter = i -> i.getLast90DaysSales(); break;
            case "maxMonthlySales": getter = i -> i.getMaxMonthlySales(); break;
            case "overseasWarehouseStock": getter = i -> i.getOverseasWarehouseStock(); break;
            case "overseasWarehouseAge": getter = i -> i.getOverseasWarehouseAge(); break;
            case "stockSalesRatio": getter = i -> i.getStockSalesRatio(); break;
            case "estimatedReplenish": getter = i -> i.getEstimatedReplenish(); break;
            case "brand": getter = DailyPriceTrackingItem::getBrand; break;
            case "operator": getter = DailyPriceTrackingItem::getOperator; break;
            default: return null;
        }
        Comparator<DailyPriceTrackingItem> cmp = asc
                ? Comparator.comparing(getter, Comparator.nullsLast(Comparator.naturalOrder()))
                : Comparator.comparing(getter, Comparator.nullsLast(Comparator.reverseOrder()));
        return cmp;
    }

    // ====================================================================
    // 核心计算逻辑
    // ====================================================================

    // ====================================================================
    // 核心计算逻辑（实时，无快照）
    // ====================================================================

    /**
     * 独立计算每日跟价数据，不再复用 InventoryOverviewService。
     * 数据流程：基准 SKU → 库存 → 销量 → 出库时间 → 组装。
     */
    private List<DailyPriceTrackingItem> computeDailyPriceTracking() {

        List<Integer> inventoryWids = engine.parseInventoryWids();
        Map<Integer, WarehouseEntity> warehouseMap = engine.loadWarehouseMap(inventoryWids);
        Map<Integer, String> widToSite = engine.buildWidToSite(warehouseMap);

        // ==== 2. 基准 (baseSku → 站点列表 + 产品名 + OE) from ebay_product_dedup（已去重） ====
        Map<String, String> skuProductNameMap = new LinkedHashMap<>();
        Map<String, Set<String>> skuSitesMap = new LinkedHashMap<>(); // baseSku → Set<siteLabel>
        Map<String, java.math.BigDecimal> dedupReturnRateMap = new LinkedHashMap<>(); // "baseSku|siteLabel" → returnRate
        for (EbayProductDedupEntity dedup : dedupService.listAll()) {
            String baseSku = dedup.getSku();
            String siteLabel = dedup.getSite();
            if (baseSku == null || baseSku.isEmpty() || siteLabel == null || siteLabel.isEmpty()) continue;
            if (!skuProductNameMap.containsKey(baseSku) && dedup.getProductName() != null) {
                skuProductNameMap.put(baseSku, dedup.getProductName().trim());
            }
            skuSitesMap.computeIfAbsent(baseSku, k -> new LinkedHashSet<>()).add(siteLabel);
            if (dedup.getReturnRate() != null) {
                dedupReturnRateMap.put(baseSku + "|" + siteLabel, dedup.getReturnRate());
            }
        }

        // ==== 3. 海外可售库存 (仅 type=3 海外仓) ====
        Map<String, Integer> overseasStockMap = new LinkedHashMap<>(); // "baseSku|siteLabel" → stock
        for (WarehouseInventoryDetailEntity d : inventoryService.lambdaQuery()
                .in(WarehouseInventoryDetailEntity::getWid, inventoryWids).list()) {
            WarehouseEntity wh = warehouseMap.get(d.getWid());
            if (wh == null || wh.getType() == null || wh.getType() != 3) continue;
            String baseSku = InventoryUtils.extractBaseSku(d.getSku());
            if (baseSku.isEmpty()) continue;
            String siteLabel = widToSite.getOrDefault(d.getWid(), "");
            if (siteLabel.isEmpty()) continue;
            int sellable = d.getProductValidNum() != null ? d.getProductValidNum() : 0;
            overseasStockMap.merge(baseSku + "|" + siteLabel, sellable, Integer::sum);
        }

        // ==== 4. 销量（复用 InventoryComputeEngine） ====
        LocalDate today = LocalDate.now();
        InventoryComputeEngine.SalesAggregation salesAgg = engine.aggregateSales(today, false);
        Map<String, Integer> sales3d = salesAgg.sales3d;
        Map<String, Integer> sales7d = salesAgg.sales7d;
        Map<String, Integer> sales30d = salesAgg.sales30d;
        Map<String, Integer> sales90d = salesAgg.sales90d;
        Map<String, Map<String, Integer>> monthlySalesMap = salesAgg.monthlySales;

        // ==== 5. 品牌归属 ====
        Map<String, String> ownerByBrand = engine.loadBrandOwners();

        // ==== 5b. 利润率直接查 inventory_overview 表 ====
        Map<String, BigDecimal> profitRateMap = new LinkedHashMap<>();
        for (InventoryOverviewEntity e : overviewMapper.selectList(null)) {
            if (e.getWarehouseNames() != null && e.getSku() != null && e.getLast30DaysProfit() != null) {
                profitRateMap.put(e.getWarehouseNames() + "|" + e.getSku(), e.getLast30DaysProfit());
            }
        }

        // ==== 6. 出库时间（复用 InventoryComputeEngine） ====
        Map<String, String> outboundTimeMap = engine.computeOutboundTimes();

        // ==== 8. 组装结果 ====
        List<DailyPriceTrackingItem> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (Map.Entry<String, Set<String>> entry : skuSitesMap.entrySet()) {
            String baseSku = entry.getKey();
            for (String siteLabel : entry.getValue()) {
                if (!seen.add(baseSku + "|" + siteLabel)) continue;

                DailyPriceTrackingItem item = new DailyPriceTrackingItem();
                item.setSite(siteLabel);
                item.setSku(baseSku);
                item.setProductName(skuProductNameMap.getOrDefault(baseSku, ""));

                // 销量（按 middleCode 匹配）
                String mid = InventoryUtils.extractMiddleCode(baseSku);
                String salesKey = siteLabel + "|" + mid;
                item.setLast3DaysSales(sales3d.getOrDefault(salesKey, 0));
                item.setLast7DaysSales(sales7d.getOrDefault(salesKey, 0));
                item.setLast30DaysSales(sales30d.getOrDefault(salesKey, 0));
                item.setLast90DaysSales(sales90d.getOrDefault(salesKey, 0));
                item.setMaxMonthlySales(monthlySalesMap.containsKey(salesKey)
                        ? monthlySalesMap.get(salesKey).values().stream().max(Integer::compareTo).orElse(null)
                        : null);

                // 库存
                String stockKey = baseSku + "|" + siteLabel;
                int overseasStock = overseasStockMap.getOrDefault(stockKey, 0);
                item.setOverseasWarehouseStock(overseasStock);

                // 库销比
                int d30 = item.getLast30DaysSales();
                // 库销比 = 海外仓库存 / 近30天销量 * 100（百分比），四舍五入2位小数
                BigDecimal ratio = InventoryUtils.safeDivide(overseasStock, d30);
                item.setStockSalesRatio(ratio.compareTo(BigDecimal.ZERO) > 0
                        ? ratio.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO);

                // 海外仓库龄（取该站点下所有仓库的最新出库时间 → 距今多少天）
                if (!mid.isEmpty() && !siteLabel.isEmpty()) {
                    String latestTime = null;
                    for (Map.Entry<Integer, String> e : widToSite.entrySet()) {
                        if (siteLabel.equals(e.getValue())) {
                            String t = outboundTimeMap.get(mid + "|" + e.getKey());
                            if (t != null && (latestTime == null || t.compareTo(latestTime) > 0))
                                latestTime = t;
                        }
                    }
                    if (latestTime != null) {
                        try {
                            int days = (int) java.time.temporal.ChronoUnit.DAYS.between(
                                    LocalDate.parse(latestTime), LocalDate.now());
                            item.setOverseasWarehouseAge(days);
                        } catch (Exception ignored) {}
                    }
                }

                // 预估补货量：直接从补货页采购数量匹配（按站点+去除PC前缀的SKU）

                // SKU 等级：从补货页 inventory_overview 取利润率
                double profitRate = profitRateMap.getOrDefault(siteLabel + "|" + baseSku, BigDecimal.ZERO).doubleValue();
                item.setSkuLevel(InventoryUtils.calcProductLevel(d30, profitRate));

                // 品牌 & 操作员
                item.setBrand(InventoryUtils.extractBrandPrefix(baseSku));
                item.setOperator(InventoryUtils.matchOwner(baseSku, ownerByBrand));

                // 预留字段
                item.setOurLowestPrice(null);  // 后面批量填充
                item.setReturnRate(dedupReturnRateMap.get(baseSku + "|" + siteLabel));
                // 跟卖价/利润率/底线价/备注：查询时从 ebay_product_dedup 实时取
                // OE 号：从去重表获取（Step 11 批量填充）

                // eBay 售前/售后链接（从链接模板表取，{oe} 替换为实际 OE 号）
                String oe = item.getOeNumber();
                item.setEbayFrontpageUrl(linkTemplateService.buildPresaleUrl(siteLabel, oe != null ? oe : ""));
                item.setFrontpageSoldUrl(linkTemplateService.buildSoldUrl(siteLabel, oe != null ? oe : ""));
                item.setRemark(null);  // 查询时从 dedup 实时取

                result.add(item);
            }
        }

        // ==== 9. 从补货页匹配采购数量 → 预估补货量 ====
        // 匹配键: site + "|" + stripPcPrefix(baseSku)
        // 例如每日跟价的 "2PC-BMW-30087" → 去掉PC → "BMW-30087" → 匹配补货页同站点同SKU的采购数量
        Map<String, Integer> purchaseQtyMap = new LinkedHashMap<>();
        try {
            for (InventoryOverviewEntity ov : overviewMapper.selectList(null)) {
                if (ov.getPurchaseQuantity() != null) {
                    purchaseQtyMap.put(ov.getWarehouseNames() + "|" + ov.getSku(), ov.getPurchaseQuantity().intValue());
                }
            }
        } catch (Exception e) {
            // 补货页数据不可用时忽略
        }
        for (DailyPriceTrackingItem item : result) {
            String matchKey = item.getSite() + "|" + InventoryUtils.stripPcPrefix(item.getSku());
            Integer qty = purchaseQtyMap.get(matchKey);
            if (qty != null) {
                item.setEstimatedReplenish(qty);
            }
        }

        // ==== 10. 批量填充最低价（从 ebay_product_dedup.lowest_price） ====
        Map<String, java.math.BigDecimal> lowestPriceMap = dedupService.batchGetLowestPrices();
        for (DailyPriceTrackingItem item : result) {
            java.math.BigDecimal lp = lowestPriceMap.get(item.getSite() + "|" + item.getSku());
            if (lp != null) {
                item.setOurLowestPrice(lp);
            }
        }

        // ==== 11. 批量填充 OE 号（从去重表，并生成链接） ====
        Map<String, String> customOeMap = dedupService.batchGetOeNumbers(
                result.stream().map(r -> r.getSite() + "|" + r.getSku()).collect(Collectors.toList()));
        for (DailyPriceTrackingItem item : result) {
            String key = item.getSite() + "|" + item.getSku();
            String customOe = customOeMap.get(key);
            if (org.springframework.util.StringUtils.hasText(customOe)) {
                item.setOeNumber(customOe);
                // 用自定义 OE 重新生成链接
                item.setEbayFrontpageUrl(linkTemplateService.buildPresaleUrl(item.getSite(), customOe));
                item.setFrontpageSoldUrl(linkTemplateService.buildSoldUrl(item.getSite(), customOe));
            }
        }

        // ==== 12. 兜底：确保每行都有链接（OE为空也用空字符串替换{oe}） ====
        for (DailyPriceTrackingItem item : result) {
            if (item.getEbayFrontpageUrl() == null) {
                String oe = item.getOeNumber();
                item.setEbayFrontpageUrl(linkTemplateService.buildPresaleUrl(item.getSite(), oe != null ? oe : ""));
            }
            if (item.getFrontpageSoldUrl() == null) {
                String oe = item.getOeNumber();
                item.setFrontpageSoldUrl(linkTemplateService.buildSoldUrl(item.getSite(), oe != null ? oe : ""));
            }
        }

        // ==== 13. 跟卖价格/利润率/底线价 = ebay_product_dedup（按 site|sku 匹配） ====
        Map<String, java.math.BigDecimal> tpMap = dedupService.batchGetTrackingPrices();
        Map<String, java.math.BigDecimal> tmMap = dedupService.batchGetTrackingProfitMargins();
        Map<String, java.math.BigDecimal> fpMap = dedupService.batchGetFloorPrices();
        for (DailyPriceTrackingItem item : result) {
            String key = item.getSite() + "|" + item.getSku();
            java.math.BigDecimal tp = tpMap.get(key);
            if (tp != null) item.setTrackingPrice(tp);
            java.math.BigDecimal tm = tmMap.get(key);
            if (tm != null) item.setTrackingProfitMargin(tm);
            java.math.BigDecimal fp = fpMap.get(key);
            if (fp != null) item.setFloorPrice(fp);
        }

        // 备注/跟卖价/利润率/底线价：查询时实时从 ebay_product_dedup 取

        return result;
    }

    // ====================================================================
    // 过滤/分页工具
    // ====================================================================

    /** 筛选：空值全部通过；逗号分隔多选则任一匹配即可，否则精确匹配 */
    private boolean matchMulti(String filter, String actual) {
        if (filter == null || filter.trim().isEmpty()) return true;
        if (filter.contains(",")) {
            return Arrays.stream(filter.split(","))
                    .map(String::trim)
                    .anyMatch(s -> s.equals(actual));
        }
        return filter.trim().equals(actual);
    }

    /** 模糊包含匹配 */
    private boolean matchContains(String keyword, String actual) {
        if (keyword == null || keyword.trim().isEmpty()) return true;
        if (actual == null) return false;
        return actual.toLowerCase().contains(keyword.trim().toLowerCase());
    }

    /** 重算并写入数据库 */
    @org.springframework.transaction.annotation.Transactional
    public void refreshTable() {
        LOG.info("==== 每日跟价数据重算写入数据库 开始 ====");
        long t = System.currentTimeMillis();
        try {
            List<DailyPriceTrackingItem> items = computeDailyPriceTracking();
            // 加载已有行（保留补货页字段不被覆盖）
            Map<String, InventoryOverviewEntity> existing = new LinkedHashMap<>();
            for (InventoryOverviewEntity e : overviewMapper.selectList(null)) {
                if (e.getWarehouseNames() != null && e.getSku() != null)
                    existing.put(e.getWarehouseNames() + "|" + e.getSku(), e);
            }
            int upserted = 0;
            Set<String> seen = new HashSet<>();
            for (DailyPriceTrackingItem item : items) {
                String key = item.getSite() + "|" + item.getSku();
                seen.add(key);
                InventoryOverviewEntity entity = existing.get(key);
                boolean isNew = (entity == null);
                if (isNew) entity = new InventoryOverviewEntity();
                // 只写入跟价字段，不覆盖补货页独有字段
                entity.setWarehouseNames(item.getSite());
                entity.setSku(item.getSku());
                entity.setProductName(item.getProductName());
                entity.setSkuLevel(item.getSkuLevel());
                entity.setLast3DaysSales(item.getLast3DaysSales());
                entity.setLast7DaysSales(item.getLast7DaysSales());
                entity.setLast30DaysSales(item.getLast30DaysSales());
                entity.setLast90DaysSales(item.getLast90DaysSales());
                entity.setMaxMonthlySales(item.getMaxMonthlySales());
                entity.setOverseasWarehouseStock(item.getOverseasWarehouseStock());
                entity.setOverseasWarehouseAge(item.getOverseasWarehouseAge());
                entity.setStockSalesRatio(item.getStockSalesRatio());
                entity.setEstimatedReplenish(item.getEstimatedReplenish());
                entity.setOurLowestPrice(item.getOurLowestPrice());
                entity.setReturnRate(item.getReturnRate());
                entity.setEbayFrontpageUrl(item.getEbayFrontpageUrl());
                entity.setFrontpageSoldUrl(item.getFrontpageSoldUrl());
                entity.setBrand(item.getBrand());
                entity.setOperator(item.getOperator());
                entity.setOeNumber(item.getOeNumber());
                if (isNew) overviewMapper.insert(entity);
                else overviewMapper.updateById(entity);
                upserted++;
            }
            // 清除旧行中不再属于跟价结果的跟价字段（行已从跟价计算中消失）
            for (Map.Entry<String, InventoryOverviewEntity> e : existing.entrySet()) {
                if (!seen.contains(e.getKey())) {
                    InventoryOverviewEntity ent = e.getValue();
                    ent.setLast3DaysSales(0); ent.setOurLowestPrice(null);
                    ent.setOverseasWarehouseStock(0); ent.setOverseasWarehouseAge(null);
                    ent.setStockSalesRatio(null); ent.setEstimatedReplenish(null);
                    ent.setEbayFrontpageUrl(""); ent.setFrontpageSoldUrl("");
                    ent.setBrand(""); ent.setOperator(""); ent.setOeNumber("");
                    overviewMapper.updateById(ent);
                }
            }
            LOG.info("==== 重算完成: {} 条 (upsert) 耗时{}ms ====", upserted, System.currentTimeMillis() - t);
        } catch (Exception e) { LOG.error("重算失败", e); }
    }

    private InventoryOverviewEntity dtoToEntity(DailyPriceTrackingItem i) {
        InventoryOverviewEntity e = new InventoryOverviewEntity();
        e.setWarehouseNames(i.getSite()); e.setSku(i.getSku()); e.setProductName(i.getProductName());
        e.setSkuLevel(i.getSkuLevel()); e.setLast3DaysSales(i.getLast3DaysSales());
        e.setLast7DaysSales(i.getLast7DaysSales()); e.setLast30DaysSales(i.getLast30DaysSales());
        e.setLast90DaysSales(i.getLast90DaysSales()); e.setMaxMonthlySales(i.getMaxMonthlySales());
        e.setOverseasWarehouseStock(i.getOverseasWarehouseStock()); e.setOverseasWarehouseAge(i.getOverseasWarehouseAge());
        e.setStockSalesRatio(i.getStockSalesRatio()); e.setEstimatedReplenish(i.getEstimatedReplenish());
        e.setOurLowestPrice(i.getOurLowestPrice());
        e.setReturnRate(i.getReturnRate()); e.setEbayFrontpageUrl(i.getEbayFrontpageUrl());
        e.setFrontpageSoldUrl(i.getFrontpageSoldUrl()); e.setBrand(i.getBrand()); e.setOperator(i.getOperator());
        e.setOeNumber(i.getOeNumber()); return e;
    }

    private DailyPriceTrackingItem entityToDto(InventoryOverviewEntity e) {
        DailyPriceTrackingItem i = new DailyPriceTrackingItem();
        i.setSite(e.getWarehouseNames()); i.setSku(e.getSku()); i.setProductName(e.getProductName());
        i.setSkuLevel(e.getSkuLevel()); i.setLast3DaysSales(e.getLast3DaysSales());
        i.setLast7DaysSales(e.getLast7DaysSales()); i.setLast30DaysSales(e.getLast30DaysSales());
        i.setLast90DaysSales(e.getLast90DaysSales()); i.setMaxMonthlySales(e.getMaxMonthlySales());
        i.setOverseasWarehouseStock(e.getOverseasWarehouseStock()); i.setOverseasWarehouseAge(e.getOverseasWarehouseAge());
        i.setStockSalesRatio(e.getStockSalesRatio()); i.setEstimatedReplenish(e.getEstimatedReplenish());
        i.setOurLowestPrice(e.getOurLowestPrice());
        i.setReturnRate(e.getReturnRate()); i.setEbayFrontpageUrl(e.getEbayFrontpageUrl());
        i.setFrontpageSoldUrl(e.getFrontpageSoldUrl()); i.setBrand(e.getBrand()); i.setOperator(e.getOperator());
        i.setOeNumber(e.getOeNumber()); return i;
    }

    @Override
    public PageResult<DailyPriceTrackingItem> search(long page, long size,
                                                      List<Map<String, String>> filters,
                                                      String sortField, String sortOrder,
                                                      String userId, String role) {
        Long tableCount = overviewMapper.selectCount(null);
        List<DailyPriceTrackingItem> allRows = (tableCount != null && tableCount > 0)
                ? overviewMapper.selectList(null).stream().map(this::entityToDto).collect(Collectors.toList())
                : computeDailyPriceTracking();

        // 从 ebay_product_dedup 补充实时跟价字段
        fillTrackingFromDedup(allRows);

        // 品牌权限过滤：非管理员只看自己负责的品牌
        if (!"admin".equalsIgnoreCase(role != null ? role.trim() : "") && StringUtils.hasText(userId)) {
            Set<String> userBrands = loadUserBrandCodes(userId);
            if (!userBrands.isEmpty()) {
                allRows = allRows.stream().filter(item ->
                    item.getBrand() != null && userBrands.contains(item.getBrand().toUpperCase())
                ).collect(Collectors.toList());
            }
        }

        // 多字段筛选
        if (filters != null && !filters.isEmpty()) {
            for (Map<String, String> f : filters) {
                String field = f.get("field"), val = f.get("value");
                if (field == null || val == null || val.isEmpty()) continue;
                String raw = val.trim();
                if (isNumericKey(field)) {
                    final String op; final double target;
                    String[] parsed = parseNumFilter(raw, isPercentKey(field));
                    if (parsed != null) {
                        op = parsed[0]; target = Double.parseDouble(parsed[1]);
                    } else {
                        try {
                            double v = Double.parseDouble(raw);
                            if (isPercentKey(field)) v /= 100.0;
                            target = v; op = "=";
                        } catch (NumberFormatException e) {
                            String kw = raw.toLowerCase();
                            allRows = allRows.stream().filter(item -> {
                                String fv = getFieldStr(item, field);
                                return fv != null && fv.toLowerCase().contains(kw);
                            }).collect(Collectors.toList());
                            continue;
                        }
                    }
                    allRows = allRows.stream().filter(item -> {
                        Double dv = getNumVal(item, field);
                        if (dv == null) return false;
                        switch (op) {
                            case ">": return dv > target; case ">=": return dv >= target;
                            case "<": return dv < target; case "<=": return dv <= target;
                            case "=": return Math.abs(dv - target) < 1e-10;
                            default: return false;
                        }
                    }).collect(Collectors.toList());
                    continue;
                }
                // 文本筛选
                if (raw.contains(",")) {
                    Set<String> vals = Arrays.stream(raw.split(",")).map(String::trim)
                            .filter(s -> !s.isEmpty()).map(String::toLowerCase).collect(Collectors.toSet());
                    allRows = allRows.stream().filter(item -> {
                        String fv = getFieldStr(item, field);
                        return fv != null && vals.contains(fv.toLowerCase());
                    }).collect(Collectors.toList());
                } else {
                    String kw = raw.toLowerCase();
                    allRows = allRows.stream().filter(item -> {
                        String fv = getFieldStr(item, field);
                        return fv != null && fv.toLowerCase().contains(kw);
                    }).collect(Collectors.toList());
                }
            }
        }

        // 排序
        if (sortField != null && !sortField.isEmpty() && sortOrder != null && !sortOrder.isEmpty()) {
            boolean asc = "asc".equalsIgnoreCase(sortOrder.trim());
            Comparator<DailyPriceTrackingItem> cmp = getSortComparator(sortField.trim(), asc);
            if (cmp != null) allRows.sort(cmp);
        }

        // 分页
        long p = page <= 0 ? 1 : page;
        long s = size <= 0 ? 20 : size;
        long total = allRows.size();
        int from = (int) ((p - 1) * s);
        int to = (int) Math.min(from + s, total);
        return new PageResult<>(total, p, s, from < total ? allRows.subList(from, to) : Collections.emptyList());
    }

    @Override
    public List<String> searchDistinctValues(String field, String keyword) {
        Long tableCount = overviewMapper.selectCount(null);
        List<DailyPriceTrackingItem> all = (tableCount != null && tableCount > 0)
                ? overviewMapper.selectList(null).stream().map(this::entityToDto).collect(Collectors.toList())
                : computeDailyPriceTracking();
        String kw = keyword != null && !keyword.isEmpty() ? keyword.trim().toLowerCase() : "";
        return all.stream()
                .map(item -> getFieldStr(item, field))
                .filter(v -> v != null && !v.isEmpty() && (kw.isEmpty() || v.toLowerCase().contains(kw)))
                .distinct().sorted().limit(50).collect(Collectors.toList());
    }

    @Override
    public List<String> searchDistinctValuesFiltered(String field, String keyword, String userId, String role) {
        Long tableCount = overviewMapper.selectCount(null);
        List<DailyPriceTrackingItem> all = (tableCount != null && tableCount > 0)
                ? overviewMapper.selectList(null).stream().map(this::entityToDto).collect(Collectors.toList())
                : computeDailyPriceTracking();
        // 品牌权限过滤
        if (!"admin".equalsIgnoreCase(role != null ? role.trim() : "") && StringUtils.hasText(userId)) {
            Set<String> userBrands = loadUserBrandCodes(userId);
            if (!userBrands.isEmpty()) {
                all = all.stream().filter(item ->
                    item.getBrand() != null && userBrands.contains(item.getBrand().toUpperCase())
                ).collect(Collectors.toList());
            }
        }
        String kw = keyword != null && !keyword.isEmpty() ? keyword.trim().toLowerCase() : "";
        return all.stream()
                .map(item -> getFieldStr(item, field))
                .filter(v -> v != null && !v.isEmpty() && (kw.isEmpty() || v.toLowerCase().contains(kw)))
                .distinct().sorted().limit(50).collect(Collectors.toList());
    }

    private static final Set<String> NUM_KEYS = new HashSet<>(Arrays.asList(
        "ourLowestPrice","trackingPrice","trackingProfitMargin","floorPrice","returnRate",
        "last3DaysSales","last7DaysSales","last30DaysSales","last90DaysSales","maxMonthlySales",
        "overseasWarehouseStock","overseasWarehouseAge","stockSalesRatio","estimatedReplenish"
    ));
    private static final Set<String> PCT_KEYS = new HashSet<>(Arrays.asList(
        "trackingProfitMargin","returnRate","stockSalesRatio"
    ));
    private boolean isNumericKey(String f) { return NUM_KEYS.contains(f); }
    private boolean isPercentKey(String f) { return PCT_KEYS.contains(f); }

    private String[] parseNumFilter(String raw, boolean isPercent) {
        if (raw == null || raw.isEmpty()) return null;
        String s = raw.trim(), op, ns;
        if (s.startsWith(">=")) { op = ">="; ns = s.substring(2).trim(); }
        else if (s.startsWith("<=")) { op = "<="; ns = s.substring(2).trim(); }
        else if (s.startsWith(">")) { op = ">"; ns = s.substring(1).trim(); }
        else if (s.startsWith("<")) { op = "<"; ns = s.substring(1).trim(); }
        else if (s.startsWith("=")) { op = "="; ns = s.substring(1).trim(); }
        else return null;
        if (ns.isEmpty()) return null;
        try {
            double v = Double.parseDouble(ns);
            if (isPercent) v /= 100.0;
            return new String[]{ op, String.valueOf(v) };
        } catch (NumberFormatException e) { return null; }
    }

    private Double getNumVal(DailyPriceTrackingItem i, String f) {
        switch (f) {
            case "ourLowestPrice": return i.getOurLowestPrice() != null ? i.getOurLowestPrice().doubleValue() : null;
            case "trackingPrice": return i.getTrackingPrice() != null ? i.getTrackingPrice().doubleValue() : null;
            case "trackingProfitMargin": return i.getTrackingProfitMargin() != null ? i.getTrackingProfitMargin().doubleValue() : null;
            case "floorPrice": return i.getFloorPrice() != null ? i.getFloorPrice().doubleValue() : null;
            case "returnRate": return i.getReturnRate() != null ? i.getReturnRate().doubleValue() : null;
            case "last3DaysSales": return i.getLast3DaysSales() != null ? i.getLast3DaysSales().doubleValue() : null;
            case "last7DaysSales": return i.getLast7DaysSales() != null ? i.getLast7DaysSales().doubleValue() : null;
            case "last30DaysSales": return i.getLast30DaysSales() != null ? i.getLast30DaysSales().doubleValue() : null;
            case "last90DaysSales": return i.getLast90DaysSales() != null ? i.getLast90DaysSales().doubleValue() : null;
            case "maxMonthlySales": return i.getMaxMonthlySales() != null ? i.getMaxMonthlySales().doubleValue() : null;
            case "overseasWarehouseStock": return i.getOverseasWarehouseStock() != null ? i.getOverseasWarehouseStock().doubleValue() : null;
            case "overseasWarehouseAge": return i.getOverseasWarehouseAge() != null ? i.getOverseasWarehouseAge().doubleValue() : null;
            case "stockSalesRatio": return i.getStockSalesRatio() != null ? i.getStockSalesRatio().doubleValue() : null;
            case "estimatedReplenish": return i.getEstimatedReplenish() != null ? i.getEstimatedReplenish().doubleValue() : null;
            default: return null;
        }
    }

    private Set<String> loadUserBrandCodes(String uid) { return engine.loadUserBrandCodes(uid); }

    /** 从 ebay_product_dedup 实时填充跟卖价/利润率/底线价/备注，绕过缓存表 */
    private void fillTrackingFromDedup(List<DailyPriceTrackingItem> rows) {
        if (rows == null || rows.isEmpty()) return;
        Map<String, java.math.BigDecimal> tpMap = dedupService.batchGetTrackingPrices();
        Map<String, java.math.BigDecimal> tmMap = dedupService.batchGetTrackingProfitMargins();
        Map<String, java.math.BigDecimal> fpMap = dedupService.batchGetFloorPrices();
        List<String> keys = rows.stream().map(r -> r.getSite() + "|" + r.getSku()).collect(java.util.stream.Collectors.toList());
        Map<String, String> remarkMap = dedupService.batchGetRemarks(keys);
        for (DailyPriceTrackingItem row : rows) {
            String key = row.getSite() + "|" + row.getSku();
            row.setTrackingPrice(tpMap.get(key));
            row.setTrackingProfitMargin(tmMap.get(key));
            row.setFloorPrice(fpMap.get(key));
            row.setRemark(remarkMap.get(key));
        }
    }

    private String getFieldStr(DailyPriceTrackingItem i, String f) {
        switch (f) {
            case "site": return i.getSite();
            case "sku": return i.getSku();
            case "skuLevel": return i.getSkuLevel();
            case "productName": return i.getProductName();
            case "oeNumber": return i.getOeNumber();
            case "brand": return i.getBrand();
            case "operator": return i.getOperator();
            case "remark": return i.getRemark();
            case "ebayFrontpageUrl": return i.getEbayFrontpageUrl();
            case "frontpageSoldUrl": return i.getFrontpageSoldUrl();
            default: {
                Double dv = getNumVal(i, f);
                return dv != null ? dv.toString() : null;
            }
        }
    }
}
