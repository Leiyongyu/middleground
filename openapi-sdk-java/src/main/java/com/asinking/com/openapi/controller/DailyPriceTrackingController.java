package com.asinking.com.openapi.controller;

import com.asinking.com.openapi.common.response.PageResult;
import com.asinking.com.openapi.common.response.Result;
import com.asinking.com.openapi.entity.DailyPriceTrackingEntity;
import com.asinking.com.openapi.service.DailyPriceTrackingService;
import com.asinking.com.openapi.utils.ExcelUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 每日跟价控制器：分页查询、Excel 导出。
 * 数据来源于运营总览（补货页），与 InventoryOverview 共享同一数据源。
 */
@RestController
@RequestMapping("/api/daily-price-tracking")
public class DailyPriceTrackingController {

    private final DailyPriceTrackingService service;

    public DailyPriceTrackingController(DailyPriceTrackingService service) {
        this.service = service;
    }

    /** 分页查询 */
    @GetMapping
    public Result<PageResult<DailyPriceTrackingEntity>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String site,
            @RequestParam(required = false) String sku,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String operator) {
        return Result.ok(service.page(page, size, site, sku, brand, operator));
    }

    /** Excel 导出（导出当前筛选条件下的全部数据） */
    @GetMapping("/export")
    public void export(@RequestParam(required = false) String site,
                       @RequestParam(required = false) String sku,
                       @RequestParam(required = false) String brand,
                       @RequestParam(required = false) String operator,
                       HttpServletResponse response) throws Exception {
        // 取全量（page=1, size=Integer.MAX_VALUE）
        PageResult<DailyPriceTrackingEntity> result = service.page(1, Integer.MAX_VALUE, site, sku, brand, operator);
        List<DailyPriceTrackingEntity> records = result.getRecords();

        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("每日跟价");
        String[] headers = {"站点", "SKU等级", "SKU", "我们的链接当前最低价", "跟卖价格",
                "跟卖价格利润率", "底线价", "退货率", "近3天销量", "近7天销量", "近30天销量",
                "近90天销量", "历史1个月的最大月销", "eBay前台首页", "前台已售页面",
                "SKU的海外仓库存", "SKU的海外仓库龄", "SKU库销比", "预估补货量", "品牌", "操作员", "备注"};
        CellStyle headerStyle = ExcelUtils.createHeaderStyle(wb);
        ExcelUtils.writeHeader(sheet, headerStyle, headers);

        int rowIdx = 1;
        for (DailyPriceTrackingEntity e : records) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(nvl(e.getSite()));
            row.createCell(1).setCellValue(nvl(e.getSkuLevel()));
            row.createCell(2).setCellValue(nvl(e.getSku()));
            row.createCell(3).setCellValue(e.getOurLowestPrice() != null ? e.getOurLowestPrice().doubleValue() : 0);
            row.createCell(4).setCellValue(e.getTrackingPrice() != null ? e.getTrackingPrice().doubleValue() : 0);
            row.createCell(5).setCellValue(e.getTrackingProfitMargin() != null ? e.getTrackingProfitMargin().doubleValue() : 0);
            row.createCell(6).setCellValue(e.getFloorPrice() != null ? e.getFloorPrice().doubleValue() : 0);
            row.createCell(7).setCellValue(e.getReturnRate() != null ? e.getReturnRate().doubleValue() : 0);
            row.createCell(8).setCellValue(e.getLast3DaysSales() != null ? e.getLast3DaysSales() : 0);
            row.createCell(9).setCellValue(e.getLast7DaysSales() != null ? e.getLast7DaysSales() : 0);
            row.createCell(10).setCellValue(e.getLast30DaysSales() != null ? e.getLast30DaysSales() : 0);
            row.createCell(11).setCellValue(e.getLast90DaysSales() != null ? e.getLast90DaysSales() : 0);
            row.createCell(12).setCellValue(e.getMaxMonthlySales() != null ? e.getMaxMonthlySales() : 0);
            row.createCell(13).setCellValue(nvl(e.getEbayFrontpageUrl()));
            row.createCell(14).setCellValue(nvl(e.getFrontpageSoldUrl()));
            row.createCell(15).setCellValue(e.getOverseasWarehouseStock() != null ? e.getOverseasWarehouseStock() : 0);
            row.createCell(16).setCellValue(e.getOverseasWarehouseAge() != null ? e.getOverseasWarehouseAge() : 0);
            row.createCell(17).setCellValue(e.getStockSalesRatio() != null ? e.getStockSalesRatio().doubleValue() : 0);
            row.createCell(18).setCellValue(e.getEstimatedReplenish() != null ? e.getEstimatedReplenish() : 0);
            row.createCell(19).setCellValue(nvl(e.getBrand()));
            row.createCell(20).setCellValue(nvl(e.getOperator()));
            row.createCell(21).setCellValue(nvl(e.getRemark()));
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" +
                URLEncoder.encode("每日跟价导出.xlsx", StandardCharsets.UTF_8.toString()));
        OutputStream os = response.getOutputStream();
        wb.write(os);
        wb.close();
        os.flush();
    }

    private String nvl(String s) { return s != null ? s : ""; }
}
