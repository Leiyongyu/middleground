package com.asinking.com.openapi.service;

import com.asinking.com.openapi.config.LingxingProperties;
import com.asinking.com.openapi.entity.*;
import com.asinking.com.openapi.mapper.mp.*;
import com.asinking.com.openapi.utils.InventoryUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 共享计算引擎：提取 InventoryOverviewServiceImpl 和 DailyPriceTrackingServiceImpl
 * 中的重复计算逻辑，统一数据查询和聚合，消除约60%的代码重复。
 *
 * 提供：仓库映射、品牌归属、销量聚合（时间范围过滤）、出库时间、采购周期等。
 */
@Service
public class InventoryComputeEngine {

    private static final Logger LOG = LoggerFactory.getLogger(InventoryComputeEngine.class);
    private final LingxingProperties lingxingProperties;
    private final WarehouseService warehouseService;
    private final WarehouseInventoryDetailService inventoryService;
    private final BrandOwnerService brandOwnerService;
    private final GoodcangGrnListMapper grnListMapper;
    private final GoodcangGrnDetailMapper grnDetailMapper;
    private final GoodcangWarehouseMapper gcWarehouseMapper;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final WarehouseStatementMapper warehouseStatementMapper;
    private final EbaySalesMapper ebaySalesMapper;
    private final PurchasePlanMapper purchasePlanMapper;
    private final UserMapper userMapper;

    public InventoryComputeEngine(LingxingProperties lingxingProperties,
                                   WarehouseService warehouseService,
                                   WarehouseInventoryDetailService inventoryService,
                                   BrandOwnerService brandOwnerService,
                                   GoodcangGrnListMapper grnListMapper,
                                   GoodcangGrnDetailMapper grnDetailMapper,
                                   GoodcangWarehouseMapper gcWarehouseMapper,
                                   PurchaseOrderMapper purchaseOrderMapper,
                                   WarehouseStatementMapper warehouseStatementMapper,
                                   EbaySalesMapper ebaySalesMapper,
                                   PurchasePlanMapper purchasePlanMapper,
                                   UserMapper userMapper) {
        this.lingxingProperties = lingxingProperties;
        this.warehouseService = warehouseService;
        this.inventoryService = inventoryService;
        this.brandOwnerService = brandOwnerService;
        this.grnListMapper = grnListMapper;
        this.grnDetailMapper = grnDetailMapper;
        this.gcWarehouseMapper = gcWarehouseMapper;
        this.purchaseOrderMapper = purchaseOrderMapper;
        this.warehouseStatementMapper = warehouseStatementMapper;
        this.ebaySalesMapper = ebaySalesMapper;
        this.purchasePlanMapper = purchasePlanMapper;
        this.userMapper = userMapper;
    }

    // ====================================================================
    // 缓存引用数据（避免两个 Service 各自全量查询）
    // ====================================================================

    /** 解析配置的库存仓库 WID 列表 */
    public List<Integer> parseInventoryWids() {
        String r = lingxingProperties.getInventoryWids();
        List<Integer> l = new ArrayList<>();
        if (StringUtils.hasText(r)) {
            for (String p : r.split(",")) {
                try { l.add(Integer.parseInt(p.trim())); }
                catch (NumberFormatException ignored) {}
            }
        }
        return l;
    }

    /**
     * 仓库映射：wid → warehouseEntity（排除 wid=1194）。
     * 被两个 Service 重复查询，提取为共享方法。
     */
    public Map<Integer, WarehouseEntity> loadWarehouseMap(List<Integer> wids) {
        return warehouseService.lambdaQuery()
                .in(WarehouseEntity::getWid, wids)
                .ne(WarehouseEntity::getWid, 1194)
                .list().stream()
                .collect(Collectors.toMap(WarehouseEntity::getWid, e -> e, (a, b) -> a));
    }

    /** wid → 站点标签 */
    public Map<Integer, String> buildWidToSite(Map<Integer, WarehouseEntity> warehouseMap) {
        Map<Integer, String> map = new HashMap<>();
        for (Map.Entry<Integer, WarehouseEntity> e : warehouseMap.entrySet())
            map.put(e.getKey(), InventoryUtils.whNameToSite(e.getValue().getName()));
        return map;
    }

    /** 品牌代码(大写) → 负责人姓名 */
    public Map<String, String> loadBrandOwners() {
        return brandOwnerService.list().stream().collect(Collectors.toMap(
                e -> StringUtils.hasText(e.getBrandCode()) ? e.getBrandCode().trim().toUpperCase() : "",
                e -> StringUtils.hasText(e.getOwnerName()) ? e.getOwnerName().trim() : "",
                (a, b) -> a));
    }

    /**
     * 加载用户负责的品牌代码集合（优先按 user_id 精确匹配，回退到 owner_name）。
     */
    public Set<String> loadUserBrandCodes(String userId) {
        UserEntity u = userMapper.selectById(userId);
        if (u == null) return Collections.emptySet();
        Set<String> s = new HashSet<>();
        // 先按 user_id 精确匹配（与 UserPermissionService 保持一致）
        List<BrandOwnerEntity> byUserId = null;
        try {
            byUserId = brandOwnerService.lambdaQuery()
                    .eq(BrandOwnerEntity::getUserId, u.getId()).list();
        } catch (Exception ignored) {}
        if (byUserId != null && !byUserId.isEmpty()) {
            for (BrandOwnerEntity bo : byUserId)
                if (StringUtils.hasText(bo.getBrandCode())) s.add(bo.getBrandCode().trim().toUpperCase());
        }
        // 回退：user_id 为空或列不存在时用 owner_name 匹配
        if (s.isEmpty() && StringUtils.hasText(u.getOwnerName())) {
            for (BrandOwnerEntity bo : brandOwnerService.lambdaQuery()
                    .eq(BrandOwnerEntity::getOwnerName, u.getOwnerName().trim()).list())
                if (StringUtils.hasText(bo.getBrandCode())) s.add(bo.getBrandCode().trim().toUpperCase());
        }
        return s;
    }

    /**
     * 判断 SKU 前缀是否匹配用户负责的品牌。
     */
    public boolean matchesUserBrand(String sku, Set<String> brands) {
        if (!StringUtils.hasText(sku)) return false;
        int i = sku.indexOf('-');
        return brands.contains(i > 0 ? sku.substring(0, i).toUpperCase() : sku.toUpperCase());
    }

    // ====================================================================
    // 销量聚合
    // ====================================================================

    /**
     * 按中间码+站点聚合销量。
     * @param today 当前日期
     * @param useInventoryExtraction true=使用 extractMiddleCodeForInventory（去PC前缀），
     *        false=使用 extractMiddleCode（原始第二段）
     */
    public SalesAggregation aggregateSales(LocalDate today, boolean useInventoryExtraction) {
        SalesAggregation agg = new SalesAggregation();
        LocalDate cutoff3d = today.minusDays(3);
        LocalDate cutoff7d = today.minusDays(7);
        LocalDate cutoff30d = today.minusDays(30);
        LocalDate cutoff90d = today.minusDays(90);

        for (EbaySalesEntity s : ebaySalesMapper.selectList(null)) {
            String rawSku = s.getSku(), currency = s.getCurrency();
            if (rawSku == null || rawSku.isEmpty() || currency == null || currency.isEmpty()) continue;
            String mid = useInventoryExtraction
                    ? InventoryUtils.extractMiddleCodeForInventory(rawSku)
                    : InventoryUtils.extractMiddleCode(rawSku);
            if (mid.isEmpty()) continue;
            String siteLabel = InventoryUtils.currencyToSite(currency.toUpperCase());
            if (siteLabel.isEmpty()) continue;
            LocalDate pd = s.getPaymentTime() != null ? s.getPaymentTime().toLocalDate() : null;
            if (pd == null) continue;
            int qty = s.getQuantity() != null ? s.getQuantity() : 0;
            String key = siteLabel + "|" + mid;

            if (!pd.isBefore(cutoff3d)) agg.sales3d.merge(key, qty, Integer::sum);
            if (!pd.isBefore(cutoff7d)) agg.sales7d.merge(key, qty, Integer::sum);
            if (!pd.isBefore(cutoff30d)) agg.sales30d.merge(key, qty, Integer::sum);
            if (!pd.isBefore(cutoff90d)) agg.sales90d.merge(key, qty, Integer::sum);

            String monthKey = pd.getYear() + "-" + String.format("%02d", pd.getMonthValue());
            agg.monthlySales.computeIfAbsent(key, k -> new LinkedHashMap<>())
                    .merge(monthKey, qty, Integer::sum);
        }
        return agg;
    }

    /** 销量聚合结果 */
    public static class SalesAggregation {
        public final Map<String, Integer> sales3d = new LinkedHashMap<>();
        public final Map<String, Integer> sales7d = new LinkedHashMap<>();
        public final Map<String, Integer> sales30d = new LinkedHashMap<>();
        public final Map<String, Integer> sales90d = new LinkedHashMap<>();
        public final Map<String, Map<String, Integer>> monthlySales = new LinkedHashMap<>();
    }

    // ====================================================================
    // 出库时间计算（从谷仓 GRN 数据）
    // ====================================================================

    /**
     * 从谷仓 GRN 数据计算每个 SKU+仓库 的最新出库时间。
     * 返回 map: "middleCode|wid" → 日期字符串(yyyy-MM-dd)
     */
    public Map<String, String> computeOutboundTimes() {
        List<GoodcangGrnDetailEntity> allGrnDetails = grnDetailMapper.selectList(null);
        Set<String> allReceivingCodes = new HashSet<>();
        for (GoodcangGrnDetailEntity d : allGrnDetails)
            if (d.getReceivingCode() != null) allReceivingCodes.add(d.getReceivingCode());

        Map<String, GoodcangGrnListEntity> grnListByCode = new HashMap<>();
        if (!allReceivingCodes.isEmpty())
            for (GoodcangGrnListEntity gl : grnListMapper.selectList(
                    new LambdaQueryWrapper<GoodcangGrnListEntity>()
                            .in(GoodcangGrnListEntity::getReceivingCode, allReceivingCodes)))
                grnListByCode.put(gl.getReceivingCode(), gl);

        Set<String> allWhCodes = new HashSet<>();
        for (GoodcangGrnListEntity gl : grnListByCode.values())
            if (gl.getWarehouseCode() != null) allWhCodes.add(gl.getWarehouseCode());

        Map<String, GoodcangWarehouseEntity> gcWhByCode = new HashMap<>();
        if (!allWhCodes.isEmpty())
            for (GoodcangWarehouseEntity gw : gcWarehouseMapper.selectList(
                    new LambdaQueryWrapper<GoodcangWarehouseEntity>()
                            .in(GoodcangWarehouseEntity::getWarehouseCode, allWhCodes)))
                gcWhByCode.put(gw.getWarehouseCode(), gw);

        Map<String, String> outboundTimeMap = new LinkedHashMap<>();
        for (GoodcangGrnDetailEntity d : allGrnDetails) {
            String mid = InventoryUtils.extractMiddleCodeForInventory(d.getProductSku());
            if (mid.isEmpty() || d.getReceivingCode() == null) continue;
            GoodcangGrnListEntity gl = grnListByCode.get(d.getReceivingCode());
            if (gl == null || gl.getWarehouseCode() == null || gl.getCreateAt() == null) continue;
            GoodcangWarehouseEntity gw = gcWhByCode.get(gl.getWarehouseCode());
            if (gw == null || gw.getWid() == null || gw.getWid() == 0) continue;
            String key = mid + "|" + gw.getWid();
            String dt = gl.getCreateAt().toLocalDate().toString();
            String ex = outboundTimeMap.get(key);
            if (ex == null || dt.compareTo(ex) > 0) outboundTimeMap.put(key, dt);
        }
        return outboundTimeMap;
    }

    // ====================================================================
    // 采购周期 & 待交付计算
    // ====================================================================

    /** 采购待交付数量 + 采购周期数据 */
    public PurchaseAggregation aggregatePurchases(Map<Integer, WarehouseEntity> warehouseMap) {
        PurchaseAggregation agg = new PurchaseAggregation();
        List<PurchaseOrderEntity> all = purchaseOrderMapper.selectList(null);

        for (PurchaseOrderEntity po : all) {
            String sku = po.getItemSku(), whName = po.getWareHouseName();
            if (sku == null || sku.trim().isEmpty() || whName == null || whName.trim().isEmpty()) continue;
            String site = InventoryUtils.whNameToSite(whName.trim());
            if (site.isEmpty()) continue;
            String key = site + "|" + InventoryUtils.extractInventoryGroupKey(sku.trim());

            if (po.getOrderTime() != null) {
                LocalDate od = po.getOrderTime().toLocalDate(), ex = agg.orderTimeMap.get(key);
                if (ex == null || od.isAfter(ex)) agg.orderTimeMap.put(key, od);
            }

            String statusText = po.getStatusText();
            if ("待审批".equals(statusText) || "待下单".equals(statusText))
                agg.pendingMap.merge(key, 1, Integer::sum);
        }

        // 入库时间（type=22 为入库流水）
        for (WarehouseStatementEntity ws : warehouseStatementMapper.selectList(
                new LambdaQueryWrapper<WarehouseStatementEntity>()
                        .eq(WarehouseStatementEntity::getType, 22))) {
            String sku = ws.getSku(), whName = ws.getWareHouseName();
            if (sku == null || sku.trim().isEmpty() || whName == null || whName.trim().isEmpty()
                    || ws.getOptTime() == null) continue;
            String site = InventoryUtils.whNameToSite(whName.trim());
            if (site.isEmpty()) continue;
            String key = site + "|" + InventoryUtils.extractInventoryGroupKey(sku.trim());
            LocalDate od = ws.getOptTime().toLocalDate(), ex = agg.inboundTimeMap.get(key);
            if (ex == null || od.isBefore(ex)) agg.inboundTimeMap.put(key, od);
        }

        // 计算采购周期天数
        for (String k : agg.orderTimeMap.keySet()) {
            LocalDate od = agg.orderTimeMap.get(k), ib = agg.inboundTimeMap.get(k);
            if (od != null && ib != null && !ib.isBefore(od))
                agg.cycleMap.put(k, (int) java.time.temporal.ChronoUnit.DAYS.between(od, ib));
        }

        // 采购计划（待审批）
        for (PurchasePlanEntity pp : purchasePlanMapper.selectList(
                new LambdaQueryWrapper<PurchasePlanEntity>()
                        .eq(PurchasePlanEntity::getStatusText, "待审批"))) {
            String sku = pp.getSku(), whName = pp.getWarehouseName();
            if (sku == null || sku.trim().isEmpty() || whName == null || whName.trim().isEmpty()) continue;
            String site = InventoryUtils.whNameToSite(whName.trim());
            if (site.isEmpty()) continue;
            String key = site + "|" + InventoryUtils.extractInventoryGroupKey(sku.trim());
            agg.planCountMap.merge(key, 1, Integer::sum);
        }

        return agg;
    }

    /** 采购聚合结果 */
    public static class PurchaseAggregation {
        public final Map<String, LocalDate> orderTimeMap = new LinkedHashMap<>();
        public final Map<String, LocalDate> inboundTimeMap = new LinkedHashMap<>();
        public final Map<String, Integer> pendingMap = new LinkedHashMap<>();
        public final Map<String, Integer> cycleMap = new LinkedHashMap<>();
        public final Map<String, Integer> planCountMap = new LinkedHashMap<>();
    }

    // ====================================================================
    // 站点标签工具
    // ====================================================================

    /** 仓库名称 → 站点标签（委托 InventoryUtils） */
    public String toWarehouseLabel(WarehouseEntity wh) {
        return InventoryUtils.whNameToSite(wh.getName());
    }
}
