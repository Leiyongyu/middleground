package com.asinking.com.openapi.service.impl;

import com.alibaba.fastjson.JSON;
import com.asinking.com.openapi.config.LingxingProperties;
import com.asinking.com.openapi.dto.response.InventoryOverviewItem;
import com.asinking.com.openapi.dto.response.WarehouseOptionItem;
import com.asinking.com.openapi.entity.*;
import com.asinking.com.openapi.mapper.mp.*;
import com.asinking.com.openapi.service.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InventoryOverviewServiceImpl implements InventoryOverviewService {

    private static final Logger LOG = LoggerFactory.getLogger(InventoryOverviewServiceImpl.class);
    private final LingxingProperties properties;
    private final WarehouseService warehouseService;
    private final WarehouseInventoryDetailService inventoryService;
    private final BrandOwnerService brandOwnerService;
    private final UserService userService;
    private final EbayProductListingService listingService;
    private final ProfitReportMapper profitReportMapper;
    private final GoodcangGrnListMapper grnListMapper;
    private final GoodcangGrnDetailMapper grnDetailMapper;
    private final GoodcangWarehouseMapper gcWarehouseMapper;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final WarehouseStatementMapper warehouseStatementMapper;

    public InventoryOverviewServiceImpl(LingxingProperties properties, WarehouseService warehouseService,
            WarehouseInventoryDetailService inventoryService, BrandOwnerService brandOwnerService,
            UserService userService, EbayProductListingService listingService,
            ProfitReportMapper profitReportMapper, GoodcangGrnListMapper grnListMapper,
            GoodcangGrnDetailMapper grnDetailMapper, GoodcangWarehouseMapper gcWarehouseMapper,
            PurchaseOrderMapper purchaseOrderMapper, WarehouseStatementMapper warehouseStatementMapper) {
        this.properties = properties; this.warehouseService = warehouseService;
        this.inventoryService = inventoryService; this.brandOwnerService = brandOwnerService;
        this.userService = userService; this.listingService = listingService;
        this.profitReportMapper = profitReportMapper; this.grnListMapper = grnListMapper;
        this.grnDetailMapper = grnDetailMapper; this.gcWarehouseMapper = gcWarehouseMapper;
        this.purchaseOrderMapper = purchaseOrderMapper; this.warehouseStatementMapper = warehouseStatementMapper;
    }

    @Override
    public List<InventoryOverviewItem> buildOverview() {
        List<Integer> inventoryWids = parseInventoryWids();
        Map<Integer, WarehouseEntity> warehouseMap = warehouseService.lambdaQuery()
                .in(WarehouseEntity::getWid, inventoryWids)
                .ne(WarehouseEntity::getWid, 1194) // 排除默认仓库
                .list().stream()
                .collect(Collectors.toMap(WarehouseEntity::getWid, e -> e, (a, b) -> a));

        // ==== 1. 基准 (baseSku, site) from ebay_product_listing ====
        List<EbayProductListingEntity> listings = listingService.list();
        Map<String, String> skuProductNameMap = new LinkedHashMap<>();
        Map<String, Map<String, SkuSiteInv>> siteRowsBySku = new LinkedHashMap<>();
        Set<String> seenPairs = new HashSet<>();
        for (EbayProductListingEntity pl : listings) {
            String baseSku = pl.getSku() != null ? pl.getSku().trim() : "";
            if (baseSku.isEmpty()) continue;
            if (!skuProductNameMap.containsKey(baseSku) && pl.getLocalName() != null)
                skuProductNameMap.put(baseSku, pl.getLocalName().trim());
            String site = mapSiteName(pl.getSiteName());
            if (!seenPairs.add(baseSku + "|" + site)) continue;
            siteRowsBySku.computeIfAbsent(baseSku, k -> new LinkedHashMap<>())
                    .put(site, new SkuSiteInv(baseSku, site));
        }

        // ==== 2. 库存：按baseSku前缀聚合 ====
        for (WarehouseInventoryDetailEntity d : inventoryService.lambdaQuery()
                .in(WarehouseInventoryDetailEntity::getWid, inventoryWids).list()) {
            String baseSku = extractBaseSku(d.getSku());
            if (baseSku.isEmpty()) continue;
            WarehouseEntity wh = warehouseMap.get(d.getWid());
            if (wh == null) continue;
            String label = toWarehouseLabel(wh);
            if (label.isEmpty()) continue; // CTUAMZ 等排除的仓库跳过
            Map<String, SkuSiteInv> sm = siteRowsBySku.computeIfAbsent(baseSku, k -> new LinkedHashMap<>());
            SkuSiteInv inv = sm.computeIfAbsent(label, k -> new SkuSiteInv(baseSku, label));
            int s = d.getProductValidNum() != null ? d.getProductValidNum() : 0;
            int o = d.getProductOnway() != null ? d.getProductOnway() : 0;
            inv.lockNum += d.getProductLockNum() != null ? d.getProductLockNum() : 0;
            if (wh.getType() != null && wh.getType() == 3) { inv.overseasSellable += s; inv.overseasOnway += o; }
            else { inv.localSellable += s; inv.localOnway += o; }
        }

        // ==== 3. 利润：profit_report 近30天 ====
        Map<String, BigDecimal> profitSumMap = new LinkedHashMap<>(), profitAvgMap = new LinkedHashMap<>();
        Map<String, Integer> profitCountMap = new LinkedHashMap<>();
        LocalDate d30 = LocalDate.now().minusDays(30);
        for (ProfitReportEntity pr : profitReportMapper.selectList(null)) {
            String msku = pr.getMsku() != null ? pr.getMsku().trim() : "";
            if (msku.isEmpty() || pr.getShipTime() == null || pr.getShipTime().isEmpty()) continue;
            LocalDate sd = parseDate(pr.getShipTime());
            if (sd == null || sd.isBefore(d30)) continue;
            String site = currencyToSite(pr.getCurrencyCode() != null ? pr.getCurrencyCode().trim().toUpperCase() : "");
            if (site.isEmpty()) continue;
            String key = extractBaseSku(msku) + "|" + site;
            if (pr.getGrossMargin() != null) { profitSumMap.merge(key, pr.getGrossMargin(), BigDecimal::add); profitCountMap.merge(key, 1, Integer::sum); }
        }
        for (String k : profitSumMap.keySet())
            profitAvgMap.put(k, profitSumMap.get(k).divide(BigDecimal.valueOf(profitCountMap.getOrDefault(k, 1)), 6, RoundingMode.HALF_UP));

        // ==== 4. 谷仓出库时间 ====
        Map<String, String> createTimeMap = new LinkedHashMap<>();
        for (GoodcangGrnDetailEntity d : grnDetailMapper.selectList(null)) {
            String mid = extractMiddleCode(d.getProductSku());
            if (mid.isEmpty() || d.getReceivingCode() == null) continue;
            GoodcangGrnListEntity gl = grnListMapper.selectOne(new LambdaQueryWrapper<GoodcangGrnListEntity>().eq(GoodcangGrnListEntity::getReceivingCode, d.getReceivingCode()));
            if (gl == null || gl.getWarehouseCode() == null) continue;
            GoodcangWarehouseEntity gw = gcWarehouseMapper.selectOne(new LambdaQueryWrapper<GoodcangWarehouseEntity>().eq(GoodcangWarehouseEntity::getWarehouseCode, gl.getWarehouseCode()).last("limit 1"));
            if (gw == null || gw.getWid() == null || gw.getWid() == 0 || gl.getCreateAt() == null) continue;
            String key = mid + "|" + gw.getWid(), dt = gl.getCreateAt().toLocalDate().toString();
            String ex = createTimeMap.get(key);
            if (ex == null || dt.compareTo(ex) > 0) createTimeMap.put(key, dt);
        }
        Map<Integer, String> widToSite = new HashMap<>();
        for (WarehouseEntity wh : warehouseService.lambdaQuery().in(WarehouseEntity::getWid, inventoryWids).list())
            widToSite.put(wh.getWid(), toWarehouseLabel(wh));

        // ============================================================================
        // ===== Step 5. 采购周期 = 入库时间 − 采购下单时间，按 (站点, baseSku) 匹配 =====
        // ============================================================================
        //
        // 数据源：
        //   - 采购下单时间：purchase_order.order_time（取同一key下最新的）
        //   - 入库时间：warehouse_statement.opt_time，限 type=22（采购入库），取同一key下最早的
        //
        // 匹配键 site|baseSku 的构建：
        //   1. 从 purchase_order.item_ware_house_name 拿仓库名（如 "CTUeBay-DE中转仓"）
        //   2. whNameToSite() 把仓库名转为站点标签（如 contains("-DE") → "德国"）
        //   3. extractBaseSku() 从 SKU 提取基础码（如 "RNG-80210-0557" → "RNG-80210"）
        //   4. 拼成 key = "德国|RNG-80210"
        //
        // ----- 5a. 采购下单时间：从 purchase_order 取 -----
        // 遍历 purchase_order 表，按 (site, baseSku) 聚合，同一 key 保留最新 order_time
        Map<String, LocalDate> orderTimeMap = new LinkedHashMap<>();
        for (PurchaseOrderEntity po : purchaseOrderMapper.selectList(null)) {
            String sku = po.getItemSku(), whName = po.getItemWareHouseName();
            // 三个必要条件：SKU不为空、仓库名不为空、下单时间不为空
            if (sku == null || sku.trim().isEmpty() || whName == null || whName.trim().isEmpty() || po.getOrderTime() == null) continue;
            // 仓库名 → 站点标签（依赖 whNameToSite 的正确映射）
            String site = whNameToSite(whName.trim());
            if (site.isEmpty()) continue; // 仓库名无法映射到站点，跳过
            String key = site + "|" + extractBaseSku(sku.trim());
            LocalDate od = po.getOrderTime().toLocalDate(), ex = orderTimeMap.get(key);
            // 取最近的下单时间（isAfter → 保留更晚的日期）
            if (ex == null || od.isAfter(ex)) orderTimeMap.put(key, od);
        }

        // ----- 5b. 入库时间：从 warehouse_statement 取（仅 type=22 采购入库） -----
        // 遍历 warehouse_statement 表，按 (site, baseSku) 聚合，同一 key 保留最早 opt_time
        Map<String, LocalDate> inboundTimeMap = new LinkedHashMap<>();
        for (WarehouseStatementEntity ws : warehouseStatementMapper.selectList(
                new LambdaQueryWrapper<WarehouseStatementEntity>().eq(WarehouseStatementEntity::getType, 22))) {
            String sku = ws.getSku(), whName = ws.getWareHouseName();
            if (sku == null || sku.trim().isEmpty() || whName == null || whName.trim().isEmpty() || ws.getOptTime() == null) continue;
            String site = whNameToSite(whName.trim());
            if (site.isEmpty()) continue;
            String key = site + "|" + extractBaseSku(sku.trim());
            LocalDate od = ws.getOptTime().toLocalDate(), ex = inboundTimeMap.get(key);
            // 取最早的入库时间（isBefore → 保留更早的日期）
            if (ex == null || od.isBefore(ex)) inboundTimeMap.put(key, od);
        }

        // ----- 5c. 计算采购周期天数 = 入库时间 − 采购下单时间 -----
        // 只有当 orderTimeMap 和 inboundTimeMap 都有同一个 key 时才计算
        Map<String, Integer> purchaseCycleMap = new LinkedHashMap<>();
        for (String k : orderTimeMap.keySet()) {
            LocalDate od = orderTimeMap.get(k), ib = inboundTimeMap.get(k);
            // 入库时间必须 ≥ 下单时间（不能出现负数天数）
            if (od != null && ib != null && !ib.isBefore(od))
                purchaseCycleMap.put(k, (int) java.time.temporal.ChronoUnit.DAYS.between(od, ib));
        }

        // ==== 6. 品牌归属 ====
        Map<String, String> ownerByBrand = brandOwnerService.list().stream().collect(Collectors.toMap(
                e -> StringUtils.hasText(e.getBrandCode()) ? e.getBrandCode().trim().toUpperCase() : "",
                e -> StringUtils.hasText(e.getOwnerName()) ? e.getOwnerName().trim() : "", (a, b) -> a));

        // ==== 7. 构建结果 ====
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
                item.setLast7DaysSales(0); item.setLast30DaysSales(0); item.setLast90DaysSales(0);

                BigDecimal m = profitAvgMap.get(baseSku + "|" + inv.siteLabel);
                item.setLast30DaysProfit(m != null ? m.multiply(BigDecimal.valueOf(100)) : null);

                String mid = extractMiddleCode(baseSku);
                if (!mid.isEmpty() && !inv.siteLabel.isEmpty())
                    for (Map.Entry<Integer, String> e : widToSite.entrySet())
                        if (inv.siteLabel.equals(e.getValue())) {
                            String t = createTimeMap.get(mid + "|" + e.getKey());
                            if (t != null) { item.setLastLocalOutboundTime(t); try { item.setOutboundDays((int) java.time.temporal.ChronoUnit.DAYS.between(LocalDate.parse(t), LocalDate.now())); } catch (Exception ignored) {} }
                            break;
                        }

                Integer pc = purchaseCycleMap.get(inv.siteLabel + "|" + baseSku);
                if (pc != null) item.setPurchaseCycle(pc);
                else LOG.info("采购周期未匹配: key={}|{} siteLabel={} baseSku={} mapSize={}", inv.siteLabel, baseSku, inv.siteLabel, baseSku, purchaseCycleMap.size());

                item.setOverseasInStockRatio(divide(item.getOverseasSellable(), 1));
                item.setOverseasTotalRatio(divide(item.getOverseasTotal(), 1));
                item.setTotalInventoryRatio(divide(item.getTotalInventory(), 1));
                item.setOwner(matchOwner(baseSku, ownerByBrand));
                result.add(item);
            }
        }
        return result;
    }

    @Override
    public List<InventoryOverviewItem> filterOverview(String sku, String warehouse, String userId, String role) {
        List<InventoryOverviewItem> all = buildOverview();
        String kw = StringUtils.hasText(sku) ? sku.trim().toLowerCase() : null;
        String wh = StringUtils.hasText(warehouse) ? warehouse.trim() : null;
        Set<String> brands = null;
        if (!"admin".equalsIgnoreCase(role != null ? role.trim() : "") && StringUtils.hasText(userId))
            brands = loadUserBrandCodes(userId);
        final Set<String> fb = brands;
        return all.stream().filter(item -> {
            if (kw != null && (item.getSku() == null || !item.getSku().toLowerCase().contains(kw))) return false;
            if (wh != null && !wh.equals(item.getWarehouseNames())) return false;
            if (fb != null && !fb.isEmpty() && !matchesUserBrand(item.getSku(), fb)) return false;
            return true;
        }).collect(Collectors.toList());
    }

    @Override
    public List<WarehouseOptionItem> getWarehouseOptions() {
        List<Integer> wids = parseInventoryWids();
        if (wids.isEmpty()) return Collections.emptyList();
        Map<String, List<Integer>> g = new LinkedHashMap<>();
        for (WarehouseEntity wh : warehouseService.lambdaQuery().in(WarehouseEntity::getWid, wids).list()) {
            g.computeIfAbsent(toWarehouseLabel(wh), k -> new ArrayList<>()).add(wh.getWid());
        }
        return g.entrySet().stream().map(e -> new WarehouseOptionItem(e.getKey(), e.getValue().stream().map(String::valueOf).collect(Collectors.joining(",")))).collect(Collectors.toList());
    }

    private Set<String> loadUserBrandCodes(String uid) {
        UserEntity u = userService.getById(uid);
        if (u == null || !StringUtils.hasText(u.getOwnerName())) return Collections.emptySet();
        Set<String> s = new HashSet<>();
        for (BrandOwnerEntity bo : brandOwnerService.lambdaQuery().eq(BrandOwnerEntity::getOwnerName, u.getOwnerName().trim()).list())
            if (StringUtils.hasText(bo.getBrandCode())) s.add(bo.getBrandCode().trim().toUpperCase());
        return s;
    }
    private boolean matchesUserBrand(String sku, Set<String> brands) {
        if (!StringUtils.hasText(sku)) return false;
        int i = sku.indexOf('-');
        return brands.contains(i > 0 ? sku.substring(0, i).toUpperCase() : sku.toUpperCase());
    }
    private List<Integer> parseInventoryWids() {
        String r = properties.getInventoryWids(); List<Integer> l = new ArrayList<>();
        if (StringUtils.hasText(r)) for (String p : r.split(",")) try { l.add(Integer.parseInt(p.trim())); } catch (NumberFormatException ignored) {}
        return l;
    }
    private BigDecimal divide(int n, int d) {
        return d == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(n).divide(BigDecimal.valueOf(d), 4, RoundingMode.HALF_UP);
    }
    private String matchOwner(String sku, Map<String, String> ob) {
        if (!StringUtils.hasText(sku)) return "";
        int i = sku.indexOf('-');
        return ob.getOrDefault(i > 0 ? sku.substring(0, i).toUpperCase() : sku.toUpperCase(), "");
    }
    private String extractMiddleCode(String sku) {
        if (!StringUtils.hasText(sku)) return ""; String[] p = sku.split("-"); return p.length >= 2 ? p[1] : "";
    }
    private String extractBaseSku(String sku) {
        if (!StringUtils.hasText(sku)) return ""; String[] p = sku.split("-"); return p.length >= 2 ? p[0] + "-" + p[1] : sku;
    }
    private LocalDate parseDate(String s) { if (!StringUtils.hasText(s)) return null; try { return LocalDate.parse(s.substring(0, 10)); } catch (Exception e) { return null; } }
    private static final Map<String, String> SITE_NAME_MAP = new HashMap<>();
    static { SITE_NAME_MAP.put("ebay汽配", "美国"); SITE_NAME_MAP.put("法国", "德国"); }
    private String mapSiteName(String n) { return StringUtils.hasText(n) ? SITE_NAME_MAP.getOrDefault(n, n) : ""; }
    private static final Map<String, String> CURRENCY_TO_SITE = new HashMap<>();
    static { CURRENCY_TO_SITE.put("USD", "美国"); CURRENCY_TO_SITE.put("GBP", "英国"); CURRENCY_TO_SITE.put("EUR", "德国"); }
    private String currencyToSite(String c) { return CURRENCY_TO_SITE.getOrDefault(c, ""); }
    /**
     * 仓库名 → 站点标签（toWarehouseLabel 和数据行的站点统一用此方法）。
     *
     * 规则（按优先级）：
     *   1. CTUAMZ 开头 → 排除，返回空
     *   2. CTUeBay-XX → 按后缀：-US→美国, -DE→德国, -UK→英国
     *   3. 谷仓系列 → 按关键词：加州/新泽西→美国, 德国→德国, 英国→英国
     *
     * 例：
     *   "CTUeBay-DE中转仓"  → "德国"
     *   "CTUeBay-US中转仓"  → "美国"
     *   "CTUeBay-UK中转仓"  → "英国"
     *   "谷仓 加州区"        → "美国"
     *   "谷仓 新泽西区"      → "美国"
     *   "谷仓 德国区"        → "德国"
     *   "谷仓 英国区"        → "英国"
     *   "CTUAMZ-US3中转"    → ""（排除）
     */
    private String whNameToSite(String n) {
        if (!StringUtils.hasText(n)) return "";
        if (n.startsWith("CTUAMZ")) return "";
        if (n.contains("-US") || n.contains("加州") || n.contains("新泽西")) return "美国";
        if (n.contains("-DE") || n.contains("德国")) return "德国";
        if (n.contains("-UK") || n.contains("英国")) return "英国";
        return "";
    }
    private static final Map<String, String> COUNTRY_LABELS = new HashMap<>();
    static { COUNTRY_LABELS.put("US","美国"); COUNTRY_LABELS.put("DE","德国"); COUNTRY_LABELS.put("GB","英国"); COUNTRY_LABELS.put("FR","法国"); COUNTRY_LABELS.put("IT","意大利"); COUNTRY_LABELS.put("ES","西班牙"); COUNTRY_LABELS.put("JP","日本"); COUNTRY_LABELS.put("CA","加拿大"); COUNTRY_LABELS.put("AU","澳大利亚"); }
    private String toWarehouseLabel(WarehouseEntity wh) {
        return whNameToSite(wh.getName());
    }
    private static class SkuSiteInv { String sku, siteLabel; int overseasSellable, overseasOnway, localSellable, localOnway, lockNum; SkuSiteInv(String s, String l) { sku = s; siteLabel = l; } }
}
