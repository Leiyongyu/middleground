package com.asinking.com.openapi.controller;

import com.asinking.com.openapi.common.response.Result;
import com.asinking.com.openapi.common.annotation.OperationLog;
import com.asinking.com.openapi.dto.response.PurchasePlanCreateResponse;
import com.asinking.com.openapi.entity.InventoryOverviewEntity;
import com.asinking.com.openapi.entity.WarehouseEntity;
import com.asinking.com.openapi.mapper.mp.EbayShopListMapper;
import com.asinking.com.openapi.mapper.mp.InventoryOverviewMapper;
import com.asinking.com.openapi.service.EbayProductDedupService;
import com.asinking.com.openapi.service.LingxingPurchasePlanService;
import com.asinking.com.openapi.service.UserPermissionService;
import com.asinking.com.openapi.service.WarehouseService;
import com.asinking.com.openapi.utils.InventoryUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 采购计划接口。
 */
@RestController
@RequestMapping("/api/purchase-plan")
public class PurchasePlanController {

    private final LingxingPurchasePlanService service;
    private final EbayShopListMapper ebayShopMapper;
    private final WarehouseService warehouseService;
    private final InventoryOverviewMapper overviewMapper;
    private final EbayProductDedupService dedupService;
    private final UserPermissionService permService;
    private final HttpServletRequest request;

    public PurchasePlanController(LingxingPurchasePlanService service,
                                  EbayShopListMapper ebayShopMapper,
                                  WarehouseService warehouseService,
                                  InventoryOverviewMapper overviewMapper,
                                  EbayProductDedupService dedupService,
                                  UserPermissionService permService,
                                  HttpServletRequest request) {
        this.service = service;
        this.ebayShopMapper = ebayShopMapper;
        this.warehouseService = warehouseService;
        this.overviewMapper = overviewMapper;
        this.dedupService = dedupService;
        this.permService = permService;
        this.request = request;
    }

    /** 上传 Excel 文件并创建采购计划。 */
    @OperationLog(value = "导入", target = "采购计划上传")
    @PostMapping("/upload")
    public Result<PurchasePlanCreateResponse> upload(@RequestParam("file") MultipartFile file) throws Exception {
        return Result.ok(service.uploadAndCreate(file));
    }

    /** 根据 JSON 数据批量创建采购计划。 */
    @PostMapping("/create")
    public Result<PurchasePlanCreateResponse> create(@RequestBody List<Map<String, Object>> data) throws Exception {
        return Result.ok(service.createFromJson(data));
    }

    /** 从去重表搜索 SKU，非管理员只返回自己负责的品牌 */
    @GetMapping("/skus")
    public Result<List<Map<String, Object>>> searchSkus(@RequestParam(defaultValue = "") String keyword) {
        UserPermissionService.UserPermission perm = permService.fromRequest(request);
        Set<String> skus = new LinkedHashSet<>();
        for (var e : dedupService.listAll()) {
            String s = e.getSku();
            if (s == null || s.isEmpty()) continue;
            if (!perm.canViewSku(s)) continue;
            if (keyword.isEmpty() || s.toLowerCase().contains(keyword.toLowerCase()))
                skus.add(s);
        }
        return Result.ok(skus.stream().limit(500).map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("sku", s);
            return m;
        }).collect(Collectors.toList()));
    }

    /** 从 eBay 店铺表中搜索店铺名，用于前端下拉提示。 */
    @GetMapping("/stores")
    public Result<List<Map<String, Object>>> searchStores(@RequestParam(defaultValue = "") String keyword) {
        Map<String, String> storeMap = new LinkedHashMap<>();
        for (var e : ebayShopMapper.selectList(null)) {
            String sid = e.getSid(), name = e.getStoreName();
            if (name != null && !name.isEmpty() && !storeMap.containsKey(name)
                    && (keyword.isEmpty() || name.toLowerCase().contains(keyword.toLowerCase())))
                storeMap.put(name, sid != null ? sid : "");
        }
        return Result.ok(storeMap.entrySet().stream().limit(50).map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("sid", e.getValue());
            m.put("seller_name", e.getKey());
            return m;
        }).collect(Collectors.toList()));
    }

    /** 返回三个 CTUeBay 中转仓库：DE/US/UK。 */
    @GetMapping("/warehouses")
    public Result<List<Map<String, Object>>> searchWarehouses(@RequestParam(defaultValue = "") String keyword) {
        List<Integer> allowedWids = Arrays.asList(18676, 18675, 18674);
        List<Map<String, Object>> list = new ArrayList<>();
        for (WarehouseEntity wh : warehouseService.listByWids(allowedWids)) {
            String name = wh.getName();
            if (name == null || name.isEmpty()) continue;
            if (!keyword.isEmpty() && !name.toLowerCase().contains(keyword.toLowerCase())) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("wid", wh.getWid());
            m.put("name", name);
            list.add(m);
        }
        return Result.ok(list);
    }

    /**
     * 根据 SKU 和仓库 wid 匹配运营数据，返回近30天利润、销量和产品等级，用于自动填充备注。
     */
    @GetMapping("/product-info")
    public Result<Map<String, Object>> productInfo(@RequestParam String sku, @RequestParam Integer wid) {
        UserPermissionService.UserPermission perm = permService.fromRequest(request);
        if (!perm.canViewSku(sku)) return Result.ok(null);

        String[] parts = sku.trim().split("-");
        String baseSku = parts.length >= 2 ? parts[0] + "-" + parts[1] : sku.trim();
        WarehouseEntity wh = warehouseService.getByWid(wid);
        String siteLabel = wh != null ? whNameToSite(wh.getName()) : "";

        Map<String, Object> result = new LinkedHashMap<>();
        if (!siteLabel.isEmpty()) {
            for (InventoryOverviewEntity e : overviewMapper.selectList(null)) {
                if (baseSku.equals(e.getSku()) && siteLabel.equals(e.getWarehouseNames())) {
                    BigDecimal profitRate = e.getLast30DaysProfit();
                    int sales = e.getLast30DaysSales() != null ? e.getLast30DaysSales() : 0;
                    result.put("profitRate", profitRate);
                    result.put("sales", sales);
                    result.put("level", InventoryUtils.calcProductLevel(sales, profitRate != null ? profitRate.doubleValue() : 0));
                    result.put("maxReplenish", e.getMaxMonthlyReplenish());
                    result.put("purchaseQuantity", e.getPurchaseQuantity());
                    return Result.ok(result);
                }
            }
        }
        result.put("profitRate", null);
        result.put("sales", 0);
        result.put("level", "—");
        result.put("maxReplenish", null);
        result.put("purchaseQuantity", null);
        return Result.ok(result);
    }

    /** 仓库名称 → 站点标签，委托 InventoryUtils 统一处理。 */
    private String whNameToSite(String name) {
        return InventoryUtils.whNameToSite(name);
    }
}
