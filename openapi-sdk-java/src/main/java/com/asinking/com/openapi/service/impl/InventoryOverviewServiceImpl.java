package com.asinking.com.openapi.service.impl;

import com.asinking.com.openapi.common.response.PageResult;
import com.asinking.com.openapi.config.LingxingProperties;
import com.asinking.com.openapi.dto.request.OverviewSearchRequest;
import com.asinking.com.openapi.dto.response.InventoryOverviewItem;
import com.asinking.com.openapi.entity.*;
import com.asinking.com.openapi.mapper.mp.*;
import com.asinking.com.openapi.service.*;
import com.asinking.com.openapi.utils.InventoryUtils;
import com.asinking.com.openapi.entity.EbayProductDedupEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 库存总览服务实现：按 SKU+站点维度聚合库存、销量、库销比、采购建议，
 * 支持 admin 全量 / 普通用户按品牌负责人过滤，结果写入快照表并缓存30分钟。
 */
@Service
public class InventoryOverviewServiceImpl implements InventoryOverviewService {

    private static final Logger LOG = LoggerFactory.getLogger(InventoryOverviewServiceImpl.class);
    private final InventoryComputeEngine engine;
    private final LingxingProperties properties;
    private final WarehouseService warehouseService;
    private final WarehouseInventoryDetailService inventoryService;
    private final InventoryOverviewMapper overviewMapper;
    private final EbayProductDedupService dedupService;

    public InventoryOverviewServiceImpl(LingxingProperties properties, WarehouseService warehouseService,
            WarehouseInventoryDetailService inventoryService,
            InventoryOverviewMapper overviewMapper,
            EbayProductDedupService dedupService,
            InventoryComputeEngine engine) {
        this.properties = properties; this.warehouseService = warehouseService;
        this.inventoryService = inventoryService;
        this.overviewMapper = overviewMapper;
        this.dedupService = dedupService;
        this.engine = engine;
    }

    @Override
    public List<InventoryOverviewItem> buildOverview() {
        return overviewMapper.selectList(null).stream().map(this::entityToDto).collect(Collectors.toList());
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void refreshSnapshot() {
        LOG.info("==== 运营数据重算写入数据库 开始 ====");
        long t = System.currentTimeMillis();
        try {
            List<InventoryOverviewItem> items = computeOverview();
            // 加载已有行（保留跟价页字段不被覆盖）
            Map<String, InventoryOverviewEntity> existing = new LinkedHashMap<>();
            for (InventoryOverviewEntity e : overviewMapper.selectList(null)) {
                if (e.getWarehouseNames() != null && e.getSku() != null)
                    existing.put(e.getWarehouseNames() + "|" + e.getSku(), e);
            }
            int upserted = 0;
            Set<String> seen = new HashSet<>();
            for (InventoryOverviewItem item : items) {
                String key = item.getWarehouseNames() + "|" + item.getSku();
                seen.add(key);
                InventoryOverviewEntity entity = existing.get(key);
                boolean isNew = (entity == null);
                if (isNew) entity = new InventoryOverviewEntity();
                // 写入补货页字段
                entity.setWarehouseNames(item.getWarehouseNames());
                entity.setSku(item.getSku());
                entity.setProductName(item.getProductName());
                entity.setSkuLevel(item.getSkuLevel());
                entity.setLast30DaysProfit(item.getLast30DaysProfit());
                entity.setReturnRate(item.getReturnRate());
                entity.setOverseasOnway(item.getOverseasOnway());
                entity.setOverseasSellable(item.getOverseasSellable());
                entity.setOverseasTotal(item.getOverseasTotal());
                entity.setPurchasePendingDelivery(item.getPurchasePendingDelivery());
                entity.setLocalSellable(item.getLocalSellable());
                entity.setLocalOnway(item.getLocalOnway());
                entity.setPurchasePlan(item.getPurchasePlan());
                entity.setLockNum(item.getLockNum());
                entity.setTotalInventory(item.getTotalInventory());
                entity.setLast7DaysSales(item.getLast7DaysSales());
                entity.setLast30DaysSales(item.getLast30DaysSales());
                entity.setLast90DaysSales(item.getLast90DaysSales());
                entity.setMaxMonthlySales(item.getMaxMonthlySales());
                entity.setOverseasInStockRatio(item.getOverseasInStockRatio());
                entity.setOverseasTotalRatio(item.getOverseasTotalRatio());
                entity.setTotalInventoryRatio(item.getTotalInventoryRatio());
                entity.setLastLocalOutboundTime(item.getLastLocalOutboundTime());
                entity.setOutboundDays(item.getOutboundDays());
                entity.setPurchaseCycle(item.getPurchaseCycle());
                entity.setPurchaseQuantity(item.getPurchaseQuantity());
                entity.setMaxMonthlyReplenish(item.getMaxMonthlyReplenish());
                entity.setOwner(item.getOwner());
                if (isNew) overviewMapper.insert(entity);
                else overviewMapper.updateById(entity);
                upserted++;
            }
            // 清除旧行中不再属于补货结果的行（该行已从补货计算中消失）
            for (Map.Entry<String, InventoryOverviewEntity> e : existing.entrySet()) {
                if (!seen.contains(e.getKey())) {
                    overviewMapper.deleteById(e.getValue().getId());
                }
            }
            LOG.info("==== 重算完成: {} 条 (upsert) 耗时{}ms ====", upserted, System.currentTimeMillis() - t);
        } catch (Exception e) {
            LOG.error("重算失败", e);
        }
    }

    @Override
    public PageResult<InventoryOverviewItem> pageOverview(long page, long size, String sku, String warehouse,
                                                           String userId, String role,
                                                           String sortField, String sortOrder,
                                                           String filterField, String filterValue) {
        List<InventoryOverviewItem> filtered = filterOverview(sku, warehouse, userId, role);
        // 文本字段模糊筛选（支持逗号分隔多值精确匹配 + 单值模糊搜索）
        if (StringUtils.hasText(filterField) && StringUtils.hasText(filterValue)) {
            String raw = filterValue.trim();
            if (raw.contains(",")) {
                Set<String> vals = Arrays.stream(raw.split(",")).map(String::trim)
                        .filter(StringUtils::hasText).map(String::toLowerCase).collect(Collectors.toSet());
                filtered = filtered.stream().filter(item -> {
                    String val = getTextField(item, filterField.trim());
                    return val != null && vals.contains(val.toLowerCase());
                }).collect(Collectors.toList());
            } else {
                String kw = raw.toLowerCase();
                filtered = filtered.stream().filter(item -> {
                    String val = getTextField(item, filterField.trim());
                    return val != null && val.toLowerCase().contains(kw);
                }).collect(Collectors.toList());
            }
        }
        // 排序
        if (StringUtils.hasText(sortField) && StringUtils.hasText(sortOrder)) {
            boolean asc = "asc".equalsIgnoreCase(sortOrder.trim());
            Comparator<InventoryOverviewItem> cmp = getComparator(sortField.trim(), asc);
            if (cmp != null) filtered.sort(cmp);
        }
        long p = page <= 0 ? 1 : page;
        long s = size <= 0 ? 20 : Math.min(size, 200);
        long total = filtered.size();
        long from = (p - 1) * s;
        if (from >= total) return new PageResult<>(total, p, s, Collections.emptyList());
        long to = Math.min(from + s, total);
        return new PageResult<>(total, p, s, filtered.subList((int) from, (int) to));
    }

    @Override
    public PageResult<InventoryOverviewItem> search(OverviewSearchRequest req, String userId, String role) {
        List<InventoryOverviewItem> filtered = filterOverview(null, null, userId, role);
        // 多字段筛选
        if (req.getFilters() != null && !req.getFilters().isEmpty()) {
            for (OverviewSearchRequest.FilterItem f : req.getFilters()) {
                if (!StringUtils.hasText(f.getField()) || !StringUtils.hasText(f.getValue())) continue;
                String raw = f.getValue().trim();
                String fieldName = f.getField().trim();
                // 数值字段：尝试解析比较运算符（> < >= <= =）
                if (isNumericField(fieldName)) {
                    final String op;
                    final double target;
                    String[] parsed = parseNumericFilter(raw, isPercentageField(fieldName));
                    if (parsed != null) {
                        op = parsed[0];
                        target = Double.parseDouble(parsed[1]);
                    } else {
                        try {
                            double v = Double.parseDouble(raw);
                            if (isPercentageField(fieldName)) v /= 100.0;
                            target = v;
                            op = "=";
                        } catch (NumberFormatException e) {
                            // 不是数字也不是运算符 → 按文本模糊
                            String kw = raw.toLowerCase();
                            filtered = filtered.stream().filter(item -> {
                                String val = getTextField(item, fieldName);
                                return val != null && val.toLowerCase().contains(kw);
                            }).collect(Collectors.toList());
                            continue;
                        }
                    }
                    filtered = filtered.stream().filter(item -> {
                        Double val = getNumericValue(item, fieldName);
                        if (val == null) return false;
                        switch (op) {
                            case ">":  return val > target;
                            case ">=": return val >= target;
                            case "<":  return val < target;
                            case "<=": return val <= target;
                            case "=":  return Math.abs(val - target) < 1e-10;
                            default:   return false;
                        }
                    }).collect(Collectors.toList());
                    continue;
                }
                // 文本多选或模糊搜索
                if (raw.contains(",")) {
                    Set<String> vals = Arrays.stream(raw.split(",")).map(String::trim)
                            .filter(StringUtils::hasText).map(String::toLowerCase).collect(Collectors.toSet());
                    filtered = filtered.stream().filter(item -> {
                        String val = getTextField(item, fieldName);
                        return val != null && vals.contains(val.toLowerCase());
                    }).collect(Collectors.toList());
                } else {
                    String kw = raw.toLowerCase();
                    filtered = filtered.stream().filter(item -> {
                        String val = getTextField(item, fieldName);
                        return val != null && val.toLowerCase().contains(kw);
                    }).collect(Collectors.toList());
                }
            }
        }
        // 排序
        if (StringUtils.hasText(req.getSortField()) && StringUtils.hasText(req.getSortOrder())) {
            boolean asc = "asc".equalsIgnoreCase(req.getSortOrder().trim());
            Comparator<InventoryOverviewItem> cmp = getComparator(req.getSortField().trim(), asc);
            if (cmp != null) filtered.sort(cmp);
        }
        // 分页
        long p = req.getPage() <= 0 ? 1 : req.getPage();
        long s = req.getSize() <= 0 ? 20 : req.getSize();
        long total = filtered.size();
        long from = (p - 1) * s;
        if (from >= total) return new PageResult<>(total, p, s, Collections.emptyList());
        long to = Math.min(from + s, total);
        return new PageResult<>(total, p, s, filtered.subList((int) from, (int) to));
    }

    @Override
    public List<String> searchDistinctValues(String field, String keyword, String userId, String role) {
        List<InventoryOverviewItem> all = filterOverview(null, null, userId, role);
        String kw = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase() : "";
        return all.stream()
                .map(item -> getTextField(item, field))
                .filter(v -> v != null && !v.isEmpty() && (kw.isEmpty() || v.toLowerCase().contains(kw)))
                .distinct()
                .sorted()
                .limit(50)
                .collect(Collectors.toList());
    }

    private Comparator<InventoryOverviewItem> getComparator(String field, boolean asc) {
        java.util.function.Function<InventoryOverviewItem, Comparable> getter;
        switch (field) {
            case "warehouseNames": getter = InventoryOverviewItem::getWarehouseNames; break;
            case "sku": getter = InventoryOverviewItem::getSku; break;
            case "skuLevel": getter = InventoryOverviewItem::getSkuLevel; break;
            case "productName": getter = InventoryOverviewItem::getProductName; break;
            case "last30DaysProfit": getter = i -> i.getLast30DaysProfit(); break;
            case "returnRate": getter = i -> i.getReturnRate(); break;
            case "overseasOnway": getter = i -> i.getOverseasOnway(); break;
            case "overseasSellable": getter = i -> i.getOverseasSellable(); break;
            case "overseasTotal": getter = i -> i.getOverseasTotal(); break;
            case "purchasePendingDelivery": getter = i -> i.getPurchasePendingDelivery(); break;
            case "localSellable": getter = i -> i.getLocalSellable(); break;
            case "localOnway": getter = i -> i.getLocalOnway(); break;
            case "lockNum": getter = i -> i.getLockNum(); break;
            case "totalInventory": getter = i -> i.getTotalInventory(); break;
            case "last7DaysSales": getter = i -> i.getLast7DaysSales(); break;
            case "last30DaysSales": getter = i -> i.getLast30DaysSales(); break;
            case "last90DaysSales": getter = i -> i.getLast90DaysSales(); break;
            case "maxMonthlySales": getter = i -> i.getMaxMonthlySales(); break;
            case "overseasInStockRatio": getter = i -> i.getOverseasInStockRatio(); break;
            case "overseasTotalRatio": getter = i -> i.getOverseasTotalRatio(); break;
            case "totalInventoryRatio": getter = i -> i.getTotalInventoryRatio(); break;
            case "outboundDays": getter = i -> i.getOutboundDays(); break;
            case "purchaseCycle": getter = i -> i.getPurchaseCycle(); break;
            case "purchaseQuantity": getter = i -> i.getPurchaseQuantity(); break;
            case "maxMonthlyReplenish": getter = i -> i.getMaxMonthlyReplenish(); break;
            case "lastLocalOutboundTime": getter = InventoryOverviewItem::getLastLocalOutboundTime; break;
            case "owner": getter = InventoryOverviewItem::getOwner; break;
            case "purchasePlan": getter = InventoryOverviewItem::getPurchasePlan; break;
            default: return null;
        }
        Comparator<InventoryOverviewItem> cmp = asc
                ? Comparator.comparing(getter, Comparator.nullsLast(Comparator.naturalOrder()))
                : Comparator.comparing(getter, Comparator.nullsLast(Comparator.reverseOrder()));
        return cmp;
    }

    @Override
    public List<InventoryOverviewItem> filterOverview(String sku, String warehouse, String userId, String role) {
        List<InventoryOverviewItem> all = buildOverview();
        String kw = StringUtils.hasText(sku) ? sku.trim().toLowerCase() : null;

        // 支持逗号分隔的多仓库标签筛选（如 "美国,德国"）
        Set<String> warehouseLabels = Collections.emptySet();
        if (StringUtils.hasText(warehouse)) {
            warehouseLabels = Arrays.stream(warehouse.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toSet());
        }

        Set<String> brands = null;
        if (!"admin".equalsIgnoreCase(role != null ? role.trim() : "") && StringUtils.hasText(userId))
            brands = loadUserBrandCodes(userId);
        final Set<String> fb = brands;
        final Set<String> whLabels = warehouseLabels;
        return all.stream().filter(item -> {
            if (kw != null && (item.getSku() == null || !item.getSku().toLowerCase().contains(kw))) return false;
            if (!whLabels.isEmpty() && !whLabels.contains(item.getWarehouseNames())) return false;
            if (fb != null && !fb.isEmpty() && !matchesUserBrand(item.getSku(), fb)) return false;
            return true;
        }).collect(Collectors.toList());
    }

    // ====================================================================
    // 核心计算逻辑（仅在刷新快照时调用）
    // ====================================================================

    private List<InventoryOverviewItem> computeOverview() {
        List<Integer> inventoryWids = engine.parseInventoryWids();
        Map<Integer, WarehouseEntity> warehouseMap = engine.loadWarehouseMap(inventoryWids);
        Map<Integer, String> widToSite = engine.buildWidToSite(warehouseMap);

        // ==== 1. 基准 (groupKey, site) from ebay_product_dedup（已去重） ====
        // 使用 extractInventoryGroupKey：去掉 PC 前缀后提取 baseSku，
        // 使 2PC-BMW-30087 和 BMW-30087 归入同一商品分组
        // 同时收集 profitRate 和 returnRate，避免多次全表扫描
        Map<String, String> skuProductNameMap = new LinkedHashMap<>();
        Map<String, Map<String, SkuSiteInv>> siteRowsBySku = new LinkedHashMap<>();
        java.util.Map<String, java.math.BigDecimal> prMap = new java.util.LinkedHashMap<>();
        java.util.Map<String, java.math.BigDecimal> rrMap = new java.util.LinkedHashMap<>();
        for (EbayProductDedupEntity dedup : dedupService.listAll()) {
            String rawSku = dedup.getSku() != null ? dedup.getSku().trim() : "";
            if (rawSku.isEmpty()) continue;
            String groupKey = InventoryUtils.extractInventoryGroupKey(rawSku);
            if (groupKey.isEmpty()) continue;
            if (!skuProductNameMap.containsKey(groupKey) && dedup.getProductName() != null)
                skuProductNameMap.put(groupKey, dedup.getProductName().trim());
            String site = dedup.getSite();  // 已归一化
            if (site == null || site.isEmpty()) continue;
            siteRowsBySku.computeIfAbsent(groupKey, k -> new LinkedHashMap<>())
                    .put(site, new SkuSiteInv(groupKey, site));
            // 同时收集利润率（一次扫描完成）
            if (dedup.getProfitRate() != null) {
                String mid = InventoryUtils.extractMiddleCodeForInventory(rawSku);
                if (!mid.isEmpty()) prMap.put(site + "|" + mid, dedup.getProfitRate());
            }
            // 同时收集退货率（一次扫描完成）
            if (dedup.getReturnRate() != null) {
                String mid = InventoryUtils.extractMiddleCodeForInventory(rawSku);
                if (!mid.isEmpty()) rrMap.put(site + "|" + mid, dedup.getReturnRate());
            }
        }

        // ==== 2. 库存（使用 groupKey 归并 PC/非PC）====
        for (WarehouseInventoryDetailEntity d : inventoryService.lambdaQuery()
                .in(WarehouseInventoryDetailEntity::getWid, inventoryWids).list()) {
            String baseSku = InventoryUtils.extractInventoryGroupKey(d.getSku());
            if (baseSku.isEmpty()) continue;
            WarehouseEntity wh = warehouseMap.get(d.getWid());
            if (wh == null) continue;
            String label = engine.toWarehouseLabel(wh);
            if (label.isEmpty()) continue;
            // 只汇总已有 eBay 刊登的 SKU，不新建
            Map<String, SkuSiteInv> sm = siteRowsBySku.get(baseSku);
            if (sm == null) continue;
            SkuSiteInv inv = sm.get(label);
            if (inv == null) {
                inv = new SkuSiteInv(baseSku, label);
                sm.put(label, inv);
            }
            int s = d.getProductValidNum() != null ? d.getProductValidNum() : 0;
            int o = d.getProductOnway() != null ? d.getProductOnway() : 0;
            // 成都在途使用 quantity_receive 替代 product_onway
            int qr = d.getQuantityReceive() != null ? d.getQuantityReceive().intValue() : 0;
            inv.lockNum += d.getProductLockNum() != null ? d.getProductLockNum() : 0;
            if (wh.getType() != null && wh.getType() == 3) { inv.overseasSellable += s; inv.overseasOnway += o; }
            else { inv.localSellable += s; inv.localOnway += qr; }
        }

        // ==== 3. 谷仓出库时间 ====
        Map<String, String> createTimeMap = engine.computeOutboundTimes();

        // ==== 4. 采购周期 + 待交付 ====
        InventoryComputeEngine.PurchaseAggregation purchaseAgg = engine.aggregatePurchases(warehouseMap);

        // ==== 5. eBay 销量 ====
        LocalDate today = LocalDate.now();
        InventoryComputeEngine.SalesAggregation salesAgg = engine.aggregateSales(today, true);

        // ==== 6. 品牌归属 ====
        Map<String, String> ownerByBrand = engine.loadBrandOwners();

        // ==== 7. 采购计划（复用 purchaseAgg 中的 planCountMap）====
        Map<String, Integer> purchasePlanCountMap = purchaseAgg.planCountMap;

        // ==== 9. 构建结果 ====
        List<InventoryOverviewItem> result = new ArrayList<>();
        Set<String> fs = new HashSet<>();
        for (Map.Entry<String, Map<String, SkuSiteInv>> se : siteRowsBySku.entrySet()) {
            String baseSku = se.getKey();
            for (SkuSiteInv inv : se.getValue().values()) {
                if (!fs.add(baseSku + "|" + inv.siteLabel)) continue;
                InventoryOverviewItem item = new InventoryOverviewItem();
                item.setSku(baseSku);
                item.setProductName(skuProductNameMap.getOrDefault(baseSku, ""));
                item.setWarehouseNames(inv.siteLabel);
                item.setOverseasOnway(inv.overseasOnway); item.setOverseasSellable(inv.overseasSellable);
                item.setOverseasTotal(inv.overseasSellable + inv.overseasOnway);
                item.setLocalOnway(inv.localOnway); item.setLocalSellable(inv.localSellable);
                item.setLockNum(inv.lockNum);
                item.setTotalInventory(inv.overseasSellable + inv.overseasOnway + inv.localSellable + inv.localOnway);
                String salesKey = inv.siteLabel + "|" + InventoryUtils.extractMiddleCode(baseSku);
                item.setLast7DaysSales(salesAgg.sales7d.getOrDefault(salesKey, 0));
                item.setLast30DaysSales(salesAgg.sales30d.getOrDefault(salesKey, 0));
                item.setLast90DaysSales(salesAgg.sales90d.getOrDefault(salesKey, 0));
                item.setMaxMonthlySales(salesAgg.monthlySales.containsKey(salesKey)
                        ? salesAgg.monthlySales.get(salesKey).values().stream().max(Integer::compareTo).orElse(0)
                        : null);

                String mid = InventoryUtils.extractMiddleCode(baseSku);
                if (!mid.isEmpty() && !inv.siteLabel.isEmpty()) {
                    String latestTime = null;
                    for (Map.Entry<Integer, String> e : widToSite.entrySet())
                        if (inv.siteLabel.equals(e.getValue())) {
                            String t = createTimeMap.get(mid + "|" + e.getKey());
                            if (t != null && (latestTime == null || t.compareTo(latestTime) > 0))
                                latestTime = t;
                        }
                    if (latestTime != null) {
                        item.setLastLocalOutboundTime(latestTime);
                        try { item.setOutboundDays((int) java.time.temporal.ChronoUnit.DAYS.between(LocalDate.parse(latestTime), LocalDate.now())); } catch (Exception ignored) {}
                    }
                }

                Integer pc = purchaseAgg.cycleMap.get(inv.siteLabel + "|" + baseSku);
                if (pc != null) item.setPurchaseCycle(pc);

                item.setPurchasePendingDelivery(purchaseAgg.pendingMap.getOrDefault(inv.siteLabel + "|" + baseSku, 0));
                item.setPurchasePlan(purchasePlanCountMap.getOrDefault(inv.siteLabel + "|" + baseSku, 0));

                Integer cycle = item.getPurchaseCycle(), od = item.getOutboundDays();
                int planCount = item.getPurchasePlan() != null ? item.getPurchasePlan() : 0;
                boolean allLe2 = item.getPurchasePendingDelivery() <= 2 && item.getLocalSellable() <= 2
                        && item.getLocalOnway() <= 2 && planCount <= 2 && item.getLockNum() <= 2;
                boolean canCalc = allLe2 ? (cycle != null && od != null) : (cycle != null);
                if (canCalc) {
                    BigDecimal avg = BigDecimal.valueOf(item.getLast90DaysSales()).divide(BigDecimal.valueOf(3), 4, RoundingMode.HALF_UP);
                    double days = allLe2 ? (cycle + od) : cycle;
                    item.setPurchaseQuantity(avg.multiply(BigDecimal.valueOf(days / 30.0)).setScale(0, RoundingMode.HALF_UP));
                }

                Integer mm = item.getMaxMonthlySales();
                if (mm != null && mm > 0) {
                    item.setMaxMonthlyReplenish((int) Math.round(mm * 4.03 - item.getTotalInventory()));
                }

                int d30 = item.getLast30DaysSales();
                item.setOverseasInStockRatio(divide(item.getOverseasSellable(), d30));
                item.setOverseasTotalRatio(divide(item.getOverseasTotal(), d30));
                item.setTotalInventoryRatio(divide(item.getTotalInventory(), d30));
                item.setOwner(matchOwner(baseSku, ownerByBrand));
                result.add(item);
            }
        }
        // 填充利润率/退货率（已在第1步中一次扫描完成，此处直接使用 prMap/rrMap）
        for (InventoryOverviewItem item : result) {
            String mid = InventoryUtils.extractMiddleCode(item.getSku());
            if (mid.isEmpty()) continue;
            java.math.BigDecimal pr = prMap.get(item.getWarehouseNames() + "|" + mid);
            if (pr != null) item.setLast30DaysProfit(pr.multiply(java.math.BigDecimal.valueOf(100)));
            java.math.BigDecimal rr = rrMap.get(item.getWarehouseNames() + "|" + mid);
            if (rr != null) item.setReturnRate(rr);
            // SKU等级：在利润填充之后计算
            item.setSkuLevel(calcProductLevel(item.getLast30DaysSales(),
                    item.getLast30DaysProfit() != null ? item.getLast30DaysProfit().doubleValue() : 0));
        }
        return result;
    }

    // ====================================================================
    // 工具方法
    // ====================================================================

    private Set<String> loadUserBrandCodes(String uid) { return engine.loadUserBrandCodes(uid); }
    private boolean matchesUserBrand(String sku, Set<String> brands) { return engine.matchesUserBrand(sku, brands); }
    private BigDecimal divide(int n, int d) {
        return InventoryUtils.safeDivide(n, d);
    }
    private String matchOwner(String sku, Map<String, String> ob) {
        return InventoryUtils.matchOwner(sku, ob);
    }
    private String extractMiddleCode(String sku) {
        return InventoryUtils.extractMiddleCode(sku);
    }
    private String extractBaseSku(String sku) {
        return InventoryUtils.extractBaseSku(sku);
    }
    private String mapSiteName(String n) { return InventoryUtils.mapSiteName(n); }

    /** SKU 产品等级：委托 InventoryUtils */
    private String calcProductLevel(int sales, double profitRate) {
        return InventoryUtils.calcProductLevel(sales, profitRate);
    }

    private static class SkuSiteInv { String sku, siteLabel; int overseasSellable, overseasOnway, localSellable, localOnway, lockNum; SkuSiteInv(String s, String l) { sku = s; siteLabel = l; } }

    private InventoryOverviewItem entityToDto(InventoryOverviewEntity e) {
        InventoryOverviewItem i = new InventoryOverviewItem();
        i.setWarehouseNames(e.getWarehouseNames()); i.setSku(e.getSku()); i.setProductName(e.getProductName());
        i.setSkuLevel(e.getSkuLevel()); i.setLast30DaysProfit(e.getLast30DaysProfit()); i.setReturnRate(e.getReturnRate());
        i.setOverseasOnway(nvl(e.getOverseasOnway())); i.setOverseasSellable(nvl(e.getOverseasSellable()));
        i.setOverseasTotal(nvl(e.getOverseasTotal())); i.setPurchasePendingDelivery(nvl(e.getPurchasePendingDelivery()));
        i.setLocalSellable(nvl(e.getLocalSellable())); i.setLocalOnway(nvl(e.getLocalOnway()));
        i.setPurchasePlan(e.getPurchasePlan()); i.setLockNum(nvl(e.getLockNum())); i.setTotalInventory(nvl(e.getTotalInventory()));
        i.setLast7DaysSales(nvl(e.getLast7DaysSales())); i.setLast30DaysSales(nvl(e.getLast30DaysSales()));
        i.setLast90DaysSales(nvl(e.getLast90DaysSales())); i.setMaxMonthlySales(e.getMaxMonthlySales());
        i.setOverseasInStockRatio(e.getOverseasInStockRatio()); i.setOverseasTotalRatio(e.getOverseasTotalRatio());
        i.setTotalInventoryRatio(e.getTotalInventoryRatio()); i.setLastLocalOutboundTime(e.getLastLocalOutboundTime());
        i.setOutboundDays(e.getOutboundDays()); i.setPurchaseCycle(e.getPurchaseCycle());
        i.setPurchaseQuantity(e.getPurchaseQuantity()); i.setMaxMonthlyReplenish(e.getMaxMonthlyReplenish());
        i.setOwner(e.getOwner()); return i;
    }

    private InventoryOverviewEntity dtoToEntity(InventoryOverviewItem i) {
        InventoryOverviewEntity e = new InventoryOverviewEntity();
        e.setWarehouseNames(i.getWarehouseNames()); e.setSku(i.getSku()); e.setProductName(i.getProductName());
        e.setSkuLevel(i.getSkuLevel()); e.setLast30DaysProfit(i.getLast30DaysProfit()); e.setReturnRate(i.getReturnRate());
        e.setOverseasOnway(i.getOverseasOnway()); e.setOverseasSellable(i.getOverseasSellable());
        e.setOverseasTotal(i.getOverseasTotal()); e.setPurchasePendingDelivery(i.getPurchasePendingDelivery());
        e.setLocalSellable(i.getLocalSellable()); e.setLocalOnway(i.getLocalOnway());
        e.setPurchasePlan(i.getPurchasePlan()); e.setLockNum(i.getLockNum()); e.setTotalInventory(i.getTotalInventory());
        e.setLast7DaysSales(i.getLast7DaysSales()); e.setLast30DaysSales(i.getLast30DaysSales());
        e.setLast90DaysSales(i.getLast90DaysSales()); e.setMaxMonthlySales(i.getMaxMonthlySales());
        e.setOverseasInStockRatio(i.getOverseasInStockRatio()); e.setOverseasTotalRatio(i.getOverseasTotalRatio());
        e.setTotalInventoryRatio(i.getTotalInventoryRatio()); e.setLastLocalOutboundTime(i.getLastLocalOutboundTime());
        e.setOutboundDays(i.getOutboundDays()); e.setPurchaseCycle(i.getPurchaseCycle());
        e.setPurchaseQuantity(i.getPurchaseQuantity()); e.setMaxMonthlyReplenish(i.getMaxMonthlyReplenish());
        e.setOwner(i.getOwner()); return e;
    }

    private int nvl(Integer v) { return v != null ? v : 0; }

    private String getTextField(InventoryOverviewItem item, String field) {
        switch (field) {
            // 文本字段
            case "warehouseNames": return item.getWarehouseNames();
            case "sku": return item.getSku();
            case "productName": return item.getProductName();
            case "skuLevel": return item.getSkuLevel();
            case "lastLocalOutboundTime": return item.getLastLocalOutboundTime();
            case "owner": return item.getOwner();
            // 数值字段 → 转字符串
            case "purchasePlan": return item.getPurchasePlan() != null ? String.valueOf(item.getPurchasePlan()) : null;
            case "last30DaysProfit": return item.getLast30DaysProfit() != null ? item.getLast30DaysProfit().toString() : null;
            case "returnRate": return item.getReturnRate() != null ? item.getReturnRate().toString() : null;
            case "overseasOnway": return String.valueOf(item.getOverseasOnway());
            case "overseasSellable": return String.valueOf(item.getOverseasSellable());
            case "overseasTotal": return String.valueOf(item.getOverseasTotal());
            case "purchasePendingDelivery": return String.valueOf(item.getPurchasePendingDelivery());
            case "localSellable": return String.valueOf(item.getLocalSellable());
            case "localOnway": return String.valueOf(item.getLocalOnway());
            case "lockNum": return String.valueOf(item.getLockNum());
            case "totalInventory": return String.valueOf(item.getTotalInventory());
            case "last7DaysSales": return String.valueOf(item.getLast7DaysSales());
            case "last30DaysSales": return String.valueOf(item.getLast30DaysSales());
            case "last90DaysSales": return String.valueOf(item.getLast90DaysSales());
            case "maxMonthlySales": return item.getMaxMonthlySales() != null ? String.valueOf(item.getMaxMonthlySales()) : null;
            case "overseasInStockRatio": return item.getOverseasInStockRatio() != null ? item.getOverseasInStockRatio().toString() : null;
            case "overseasTotalRatio": return item.getOverseasTotalRatio() != null ? item.getOverseasTotalRatio().toString() : null;
            case "totalInventoryRatio": return item.getTotalInventoryRatio() != null ? item.getTotalInventoryRatio().toString() : null;
            case "outboundDays": return item.getOutboundDays() != null ? String.valueOf(item.getOutboundDays()) : null;
            case "purchaseCycle": return item.getPurchaseCycle() != null ? String.valueOf(item.getPurchaseCycle()) : null;
            case "purchaseQuantity": return item.getPurchaseQuantity() != null ? item.getPurchaseQuantity().toString() : null;
            case "maxMonthlyReplenish": return item.getMaxMonthlyReplenish() != null ? String.valueOf(item.getMaxMonthlyReplenish()) : null;
            default: return null;
        }
    }

    /** 数值字段集合 */
    private static final Set<String> NUMERIC_FIELDS_SET = new HashSet<>(Arrays.asList(
        "last30DaysProfit", "returnRate", "overseasOnway", "overseasSellable", "overseasTotal",
        "purchasePendingDelivery", "localSellable", "localOnway", "purchasePlan", "lockNum",
        "totalInventory", "last7DaysSales", "last30DaysSales", "last90DaysSales", "maxMonthlySales",
        "overseasInStockRatio", "overseasTotalRatio", "totalInventoryRatio",
        "outboundDays", "purchaseCycle", "purchaseQuantity", "maxMonthlyReplenish"
    ));

    /** 百分比字段（前端*100显示，后端存原值0~1），筛选时需/100 */
    private static final Set<String> PERCENT_FIELDS = new HashSet<>(Arrays.asList(
        "returnRate", "overseasInStockRatio", "overseasTotalRatio", "totalInventoryRatio"
    ));

    private boolean isNumericField(String field) { return NUMERIC_FIELDS_SET.contains(field); }
    private boolean isPercentageField(String field) { return PERCENT_FIELDS.contains(field); }

    private String[] parseNumericFilter(String raw, boolean isPercent) {
        if (raw == null || raw.isEmpty()) return null;
        String s = raw.trim(); String op; String numStr;
        if (s.startsWith(">=")) { op = ">="; numStr = s.substring(2).trim(); }
        else if (s.startsWith("<=")) { op = "<="; numStr = s.substring(2).trim(); }
        else if (s.startsWith(">")) { op = ">"; numStr = s.substring(1).trim(); }
        else if (s.startsWith("<")) { op = "<"; numStr = s.substring(1).trim(); }
        else if (s.startsWith("=")) { op = "="; numStr = s.substring(1).trim(); }
        else return null;
        if (numStr.isEmpty()) return null;
        try {
            double v = Double.parseDouble(numStr);
            if (isPercent) v /= 100.0;
            return new String[]{ op, String.valueOf(v) };
        } catch (NumberFormatException e) { return null; }
    }

    private Double getNumericValue(InventoryOverviewItem item, String field) {
        switch (field) {
            case "last30DaysProfit": return item.getLast30DaysProfit() != null ? item.getLast30DaysProfit().doubleValue() : null;
            case "returnRate": return item.getReturnRate() != null ? item.getReturnRate().doubleValue() : null;
            case "overseasOnway": return (double) item.getOverseasOnway();
            case "overseasSellable": return (double) item.getOverseasSellable();
            case "overseasTotal": return (double) item.getOverseasTotal();
            case "purchasePendingDelivery": return (double) item.getPurchasePendingDelivery();
            case "localSellable": return (double) item.getLocalSellable();
            case "localOnway": return (double) item.getLocalOnway();
            case "purchasePlan": return item.getPurchasePlan() != null ? item.getPurchasePlan().doubleValue() : null;
            case "lockNum": return (double) item.getLockNum();
            case "totalInventory": return (double) item.getTotalInventory();
            case "last7DaysSales": return (double) item.getLast7DaysSales();
            case "last30DaysSales": return (double) item.getLast30DaysSales();
            case "last90DaysSales": return (double) item.getLast90DaysSales();
            case "maxMonthlySales": return item.getMaxMonthlySales() != null ? item.getMaxMonthlySales().doubleValue() : null;
            case "overseasInStockRatio": return item.getOverseasInStockRatio() != null ? item.getOverseasInStockRatio().doubleValue() : null;
            case "overseasTotalRatio": return item.getOverseasTotalRatio() != null ? item.getOverseasTotalRatio().doubleValue() : null;
            case "totalInventoryRatio": return item.getTotalInventoryRatio() != null ? item.getTotalInventoryRatio().doubleValue() : null;
            case "outboundDays": return item.getOutboundDays() != null ? item.getOutboundDays().doubleValue() : null;
            case "purchaseCycle": return item.getPurchaseCycle() != null ? item.getPurchaseCycle().doubleValue() : null;
            case "purchaseQuantity": return item.getPurchaseQuantity() != null ? item.getPurchaseQuantity().doubleValue() : null;
            case "maxMonthlyReplenish": return item.getMaxMonthlyReplenish() != null ? item.getMaxMonthlyReplenish().doubleValue() : null;
            default: return null;
        }
    }
}
