package com.asinking.com.openapi.controller;

import com.asinking.com.openapi.common.response.PageResult;
import com.asinking.com.openapi.common.response.Result;
import com.asinking.com.openapi.entity.AmzInventoryOverviewEntity;
import com.asinking.com.openapi.mapper.mp.AmzInventoryOverviewMapper;
import com.asinking.com.openapi.service.AmazonComputeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/amz/inventory")
public class AmzReplenishmentController {

    private final AmzInventoryOverviewMapper mapper;
    private final AmazonComputeService computeService;
    private final com.asinking.com.openapi.mapper.mp.AmzProductCategoryMapper categoryMapper;

    // 数值字段集合（支持 > < >= <= = 操作符）
    private static final Set<String> NUMERIC_FIELDS = new HashSet<>(Arrays.asList(
        "rating","reviews","adRate","profitRate30","refundRate90","purchased",
        "domesticStock","lockNum","fbaStock","fbaInbound","totalStock",
        "sales7","sales14","sales30","sales60","speed14","speed30","speed60",
        "safetyStock","avgMonthlySales","replenishQty","shipment"
    ));

    public AmzReplenishmentController(AmzInventoryOverviewMapper mapper,
                                       AmazonComputeService computeService,
                                       com.asinking.com.openapi.mapper.mp.AmzProductCategoryMapper categoryMapper) {
        this.mapper = mapper;
        this.computeService = computeService;
        this.categoryMapper = categoryMapper;
    }

    @GetMapping
    public Result<PageResult<AmzInventoryOverviewEntity>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "100") long size) {
        long total = mapper.selectCount(null);
        long from = (page - 1) * size;
        List<AmzInventoryOverviewEntity> records = mapper.selectList(
                new LambdaQueryWrapper<AmzInventoryOverviewEntity>()
                        .last("LIMIT " + from + "," + size));
        return Result.ok(new PageResult<>(total, page, size, records));
    }

    /** 多条件筛选+排序+分页 */
    @PostMapping("/search")
    public Result<PageResult<AmzInventoryOverviewEntity>> search(@RequestBody Map<String, Object> body) {
        long page = body.get("page") != null ? ((Number) body.get("page")).longValue() : 1;
        long size = body.get("size") != null ? ((Number) body.get("size")).longValue() : 100;
        String sortField = (String) body.get("sortField");
        String sortOrder = (String) body.get("sortOrder");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> filters = (List<Map<String, String>>) body.getOrDefault("filters", Collections.emptyList());

        LambdaQueryWrapper<AmzInventoryOverviewEntity> qw = new LambdaQueryWrapper<>();

        // 多字段筛选
        if (filters != null) {
            for (Map<String, String> f : filters) {
                String field = f.get("field"), val = f.get("value");
                if (field == null || val == null || val.isEmpty()) continue;
                String dbCol = camelToSnake(field);
                if (NUMERIC_FIELDS.contains(field)) {
                    applyNumericFilter(qw, dbCol, val.trim());
                } else {
                    applyTextFilter(qw, dbCol, val.trim());
                }
            }
        }

        // 总数
        long total = mapper.selectCount(qw);

        // 排序
        String orderClause = "";
        if (StringUtils.hasText(sortField) && StringUtils.hasText(sortOrder)) {
            String dbCol = camelToSnake(sortField);
            String dir = "asc".equalsIgnoreCase(sortOrder) ? "ASC" : "DESC";
            orderClause = "ORDER BY " + dbCol + " " + dir;
        }

        // 分页
        long from = (page - 1) * size;
        String limitClause = "LIMIT " + from + "," + size;
        if (!orderClause.isEmpty()) {
            qw.last(orderClause + " " + limitClause);
        } else {
            qw.last(limitClause);
        }
        List<AmzInventoryOverviewEntity> records = mapper.selectList(qw);

        return Result.ok(new PageResult<>(total, page, size, records));
    }

    /** 筛选字段去重值（实时搜索回显） */
    @GetMapping("/distinct-values")
    public Result<List<String>> distinctValues(@RequestParam String field,
                                                @RequestParam(defaultValue = "") String keyword) {
        String kw = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase() : "";
        List<String> values = mapper.selectList(null).stream()
                .map(e -> getFieldValue(e, field))
                .filter(v -> v != null && !v.isEmpty() && (kw.isEmpty() || v.toLowerCase().contains(kw)))
                .distinct().sorted().limit(50)
                .collect(Collectors.toList());
        return Result.ok(values);
    }

    /** 保存产品分类（独立持久化，刷新不丢失） */
    @PostMapping("/save-category")
    public Result<String> saveCategory(@RequestBody Map<String, String> body) {
        Integer sid = body.get("sid") != null ? Integer.parseInt(body.get("sid")) : null;
        String sku = body.get("sellerSku");
        String cat = body.getOrDefault("category", "");
        if (sid == null || sku == null || sku.isEmpty()) {
            return Result.fail(com.asinking.com.openapi.common.response.ResultCode.BAD_REQUEST, "sid和sellerSku不能为空");
        }
        com.asinking.com.openapi.entity.AmzProductCategoryEntity exist = categoryMapper.selectOne(
                new LambdaQueryWrapper<com.asinking.com.openapi.entity.AmzProductCategoryEntity>()
                        .eq(com.asinking.com.openapi.entity.AmzProductCategoryEntity::getSid, sid)
                        .eq(com.asinking.com.openapi.entity.AmzProductCategoryEntity::getSellerSku, sku));
        if (exist != null) {
            exist.setCategory(cat);
            categoryMapper.updateById(exist);
        } else {
            com.asinking.com.openapi.entity.AmzProductCategoryEntity e = new com.asinking.com.openapi.entity.AmzProductCategoryEntity();
            e.setSid(sid); e.setSellerSku(sku); e.setCategory(cat);
            categoryMapper.insert(e);
        }
        // 同时更新 overview 快照表，保证页面切换回来无需刷新即可回显
        mapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<AmzInventoryOverviewEntity>()
                .eq(AmzInventoryOverviewEntity::getSid, sid)
                .eq(AmzInventoryOverviewEntity::getSellerSku, sku)
                .set(AmzInventoryOverviewEntity::getProductCategory, cat));
        return Result.ok("ok");
    }

    /** 导出 Excel */
    @PostMapping("/export")
    public void export(@RequestBody Map<String, Object> body,
                       jakarta.servlet.http.HttpServletResponse response) throws Exception {
        @SuppressWarnings("unchecked")
        List<String> rowKeys = (List<String>) body.getOrDefault("rowKeys", Collections.emptyList());
        @SuppressWarnings("unchecked")
        List<String> colKeys = (List<String>) body.getOrDefault("colKeys", Collections.emptyList());
        @SuppressWarnings("unchecked")
        List<String> colTitles = (List<String>) body.getOrDefault("colTitles", Collections.emptyList());
        @SuppressWarnings("unchecked")
        List<Map<String, String>> filters = (List<Map<String, String>>) body.getOrDefault("filters", Collections.emptyList());

        // 用 search 逻辑筛选全量数据
        Map<String, Object> searchBody = new LinkedHashMap<>();
        searchBody.put("page", 1L); searchBody.put("size", Integer.MAX_VALUE);
        if (!filters.isEmpty()) searchBody.put("filters", filters);
        List<AmzInventoryOverviewEntity> all = search(searchBody).getData().getRecords();

        // 选中行过滤
        if (!rowKeys.isEmpty()) {
            Set<String> keySet = new HashSet<>(rowKeys);
            all = all.stream().filter(e ->
                keySet.contains(e.getSid() + "|" + e.getSellerSku() + "|" + e.getWarehouseSku()))
                .collect(Collectors.toList());
        }

        org.apache.poi.ss.usermodel.Workbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
        org.apache.poi.ss.usermodel.Sheet sheet = wb.createSheet("Amazon补货");
        org.apache.poi.ss.usermodel.Row hr = sheet.createRow(0);
        for (int i = 0; i < Math.min(colKeys.size(), colTitles.size()); i++)
            hr.createCell(i).setCellValue(colTitles.get(i));
        int ri = 1;
        for (AmzInventoryOverviewEntity e : all) {
            org.apache.poi.ss.usermodel.Row row = sheet.createRow(ri++);
            for (int i = 0; i < colKeys.size(); i++)
                row.createCell(i).setCellValue(String.valueOf(getExportVal(e, colKeys.get(i))));
        }
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" +
                java.net.URLEncoder.encode("Amazon补货.xlsx", java.nio.charset.StandardCharsets.UTF_8));
        java.io.OutputStream os = response.getOutputStream(); wb.write(os); wb.close(); os.flush();
    }

    private Object getExportVal(AmzInventoryOverviewEntity e, String k) {
        switch (k) {
            case "sellerSku": return nvl(e.getSellerSku());
            case "store": return nvl(e.getStore());
            case "warehouseSku": return nvl(e.getWarehouseSku());
            case "warehouseName": return nvl(e.getWarehouseName());
            case "asin": return nvl(e.getAsin());
            case "principalName": return nvl(e.getPrincipalName());
            case "rating": return e.getLastStar();
            case "reviews": return e.getReviewNum();
            case "adRate": return e.getAdRate();
            case "profitRate30": return e.getProfitRate30d();
            case "refundRate90": return e.getRefundRate90d();
            case "category": return nvl(e.getProductCategory());
            case "purchased": return e.getPurchasedQty();
            case "domesticStock": return e.getDomesticStock();
            case "lockNum": return e.getPendingShip();
            case "fbStock": return e.getFbaStock();
            case "fbaOnway": return e.getFbaInbound();
            case "totalStock": return e.getTotalInventory();
            case "sales7": return e.getSales7d();
            case "sales14": return e.getSales14d();
            case "sales30": return e.getSales30d();
            case "sales60": return e.getSales60d();
            case "speed14": return e.getSalesSpeed14d();
            case "speed30": return e.getSalesSpeed30d();
            case "speed60": return e.getSalesSpeed60d();
            case "safetyStock": return e.getSafetyStock();
            case "avgMonthly": return e.getAvgMonthlySales();
            case "replenishQty": return e.getReplenishQty();
            case "shipment": return e.getShipQty();
            default: return "";
        }
    }

    private String nvl(String s) { return s != null ? s : ""; }

    @PostMapping("/refresh")
    public Result<String> refresh() {
        computeService.refreshSnapshot();
        return Result.ok("ok");
    }

    // ---- helpers ----
    private String camelToSnake(String s) {
        if (s == null || s.isEmpty()) return s;
        // 已知映射
        switch (s) {
            case "sellerSku": return "seller_sku";
            case "warehouseSku": return "warehouse_sku";
            case "warehouseName": return "warehouse_name";
            case "principalName": return "principal_name";
            case "adRate": return "ad_rate";
            case "profitRate30": return "profit_rate30d";
            case "refundRate90": return "refund_rate90d";
            case "lastStar": case "rating": return "last_star";
            case "reviewNum": case "reviews": return "review_num";
            case "fbaStock": case "fbStock": return "fba_stock";
            case "fbaInbound": case "fbaOnway": return "fba_inbound";
            case "totalStock": return "total_inventory";
            case "lockNum": return "pending_ship";
            case "purchased": return "purchased_qty";
            case "domesticStock": return "domestic_stock";
            case "sales7": return "sales7d";
            case "sales14": return "sales14d";
            case "sales30": return "sales30d";
            case "sales60": return "sales60d";
            case "speed14": return "sales_speed14d";
            case "speed30": return "sales_speed30d";
            case "speed60": return "sales_speed60d";
            case "avgMonthlySales": return "avg_monthly_sales";
            case "safetyStock": return "safety_stock";
            case "replenishQty": return "replenish_qty";
            case "shipment": return "ship_qty";
            default: return s;
        }
    }

    private void applyNumericFilter(LambdaQueryWrapper<AmzInventoryOverviewEntity> qw, String col, String val) {
        String op; double num;
        if (val.startsWith(">=")) { op = ">="; num = Double.parseDouble(val.substring(2).trim()); }
        else if (val.startsWith("<=")) { op = "<="; num = Double.parseDouble(val.substring(2).trim()); }
        else if (val.startsWith(">")) { op = ">"; num = Double.parseDouble(val.substring(1).trim()); }
        else if (val.startsWith("<")) { op = "<"; num = Double.parseDouble(val.substring(1).trim()); }
        else { op = "="; num = Double.parseDouble(val.startsWith("=") ? val.substring(1).trim() : val); }

        qw.apply(col + " " + op + " {0}", num);
    }

    private void applyTextFilter(LambdaQueryWrapper<AmzInventoryOverviewEntity> qw, String col, String val) {
        if (val.contains(",")) {
            String[] parts = val.split(",");
            String inVals = Arrays.stream(parts).map(s -> "'" + s.trim() + "'").collect(Collectors.joining(","));
            qw.apply(col + " IN (" + inVals + ")");
        } else {
            qw.apply(col + " LIKE {0}", "%" + val + "%");
        }
    }

    private String getFieldValue(AmzInventoryOverviewEntity e, String field) {
        switch (field) {
            case "sellerSku": return e.getSellerSku();
            case "warehouseSku": return e.getWarehouseSku();
            case "store": return e.getStore();
            case "warehouseName": return e.getWarehouseName();
            case "principalName": return e.getPrincipalName();
            case "asin": return e.getAsin();
            default: return "";
        }
    }
}
