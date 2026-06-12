package com.asinking.com.openapi.service.impl;

import com.asinking.com.openapi.entity.EbayProductDedupEntity;
import com.asinking.com.openapi.entity.EbayProductListingEntity;
import com.asinking.com.openapi.mapper.mp.EbayProductDedupMapper;
import com.asinking.com.openapi.service.EbayProductDedupService;
import com.asinking.com.openapi.service.EbayProductListingService;
import com.asinking.com.openapi.utils.InventoryUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.util.*;

@Service
public class EbayProductDedupServiceImpl implements EbayProductDedupService {

    private static final Logger LOG = LoggerFactory.getLogger(EbayProductDedupServiceImpl.class);
    private final EbayProductDedupMapper mapper;
    private final EbayProductListingService listingService;

    public EbayProductDedupServiceImpl(EbayProductDedupMapper mapper,
                                       EbayProductListingService listingService) {
        this.mapper = mapper;
        this.listingService = listingService;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public int rebuildFromListing() {
        LOG.info("==== 去重表重建 开始 ====");
        long t = System.currentTimeMillis();

        // 1. 读 ebay_product_listing 全量，按 (sku, 归一化site) 去重，取第一个的 product_name
        Map<String, EbayProductListingEntity> deduped = new LinkedHashMap<>();
        for (EbayProductListingEntity pl : listingService.list()) {
            String sku = pl.getSku();
            if (sku == null || sku.trim().isEmpty()) continue;
            sku = sku.trim();
            String site = InventoryUtils.mapSiteName(
                    pl.getSiteName() != null ? pl.getSiteName().trim() : "");
            if (site.isEmpty()) continue;
            String key = site + "|" + sku;
            deduped.putIfAbsent(key, pl);
        }

        // 2. 加载已有去重记录（保留所有用户维护字段）
        Map<String, EbayProductDedupEntity> existingMap = new LinkedHashMap<>();
        for (EbayProductDedupEntity e : mapper.selectList(null)) {
            if (StringUtils.hasText(e.getSite()) && StringUtils.hasText(e.getSku())) {
                existingMap.put(e.getSite() + "|" + e.getSku(), e);
            }
        }

        // 3. 删除旧记录，批量插入新记录（保留用户维护的字段）
        mapper.delete(new LambdaQueryWrapper<>());
        int count = 0;
        List<EbayProductDedupEntity> batch = new ArrayList<>();
        for (Map.Entry<String, EbayProductListingEntity> entry : deduped.entrySet()) {
            String key = entry.getKey();
            String[] parts = key.split("\\|", 2);
            EbayProductDedupEntity entity = new EbayProductDedupEntity();
            entity.setSite(parts[0]);
            entity.setSku(parts[1]);
            // 产品名称：取第一个 listing 的 local_name
            EbayProductListingEntity pl = entry.getValue();
            entity.setProductName(pl.getLocalName() != null ? pl.getLocalName().trim() : "");
            // 从旧记录恢复所有用户维护的字段
            EbayProductDedupEntity old = existingMap.get(key);
            if (old != null) {
                entity.setOeNumber(StringUtils.hasText(old.getOeNumber()) ? old.getOeNumber() : "");
                entity.setRemark(old.getRemark());
                entity.setTrackingPrice(old.getTrackingPrice());
                entity.setTrackingProfitMargin(old.getTrackingProfitMargin());
                entity.setFloorPrice(old.getFloorPrice());
                entity.setLowestPrice(old.getLowestPrice());
                entity.setLowestItemNumber(old.getLowestItemNumber());
                entity.setLowestUploadTime(old.getLowestUploadTime());
                entity.setProfitRate(old.getProfitRate());
                entity.setReturnRate(old.getReturnRate());
            }
            batch.add(entity);
            count++;

            if (batch.size() >= 1000) {
                for (EbayProductDedupEntity e : batch) mapper.insert(e);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            for (EbayProductDedupEntity e : batch) mapper.insert(e);
        }

        LOG.info("==== 去重表重建 完成: {} 条 耗时{}ms ====", count, System.currentTimeMillis() - t);
        return count;
    }

    @Override
    public void saveOe(String site, String sku, String oeNumber) {
        EbayProductDedupEntity existing = mapper.selectOne(
                new LambdaQueryWrapper<EbayProductDedupEntity>()
                        .eq(EbayProductDedupEntity::getSite, site)
                        .eq(EbayProductDedupEntity::getSku, sku));
        if (existing != null) {
            existing.setOeNumber(oeNumber);
            mapper.updateById(existing);
        }
    }

    @Override
    public Map<String, String> batchGetOeNumbers(List<String> keys) {
        Map<String, String> result = new LinkedHashMap<>();
        if (keys == null || keys.isEmpty()) return result;
        // 按 keys 过滤查询，避免全表扫描
        for (EbayProductDedupEntity e : queryByCompositeKeys(keys)) {
            if (StringUtils.hasText(e.getSite()) && StringUtils.hasText(e.getSku())) {
                result.put(e.getSite() + "|" + e.getSku(),
                        StringUtils.hasText(e.getOeNumber()) ? e.getOeNumber() : "");
            }
        }
        return result;
    }

    @Override
    public void saveRemark(String site, String sku, String remark) {
        EbayProductDedupEntity existing = mapper.selectOne(
                new LambdaQueryWrapper<EbayProductDedupEntity>()
                        .eq(EbayProductDedupEntity::getSite, site)
                        .eq(EbayProductDedupEntity::getSku, sku));
        if (existing != null) {
            existing.setRemark(remark);
            mapper.updateById(existing);
        }
    }

    @Override
    public Map<String, String> batchGetRemarks(List<String> keys) {
        Map<String, String> result = new LinkedHashMap<>();
        if (keys == null || keys.isEmpty()) return result;
        // 按 keys 过滤查询，避免全表扫描
        for (EbayProductDedupEntity e : queryByCompositeKeys(keys)) {
            if (StringUtils.hasText(e.getSite()) && StringUtils.hasText(e.getSku())) {
                result.put(e.getSite() + "|" + e.getSku(),
                        StringUtils.hasText(e.getRemark()) ? e.getRemark() : "");
            }
        }
        return result;
    }

    /** 按 "site|sku" 复合键批量查询，避免全表扫描 */
    private List<EbayProductDedupEntity> queryByCompositeKeys(List<String> keys) {
        if (keys == null || keys.isEmpty()) return Collections.emptyList();
        // 按 site 分组后用 OR 条件查询：site=US AND sku IN (a,b) OR site=DE AND sku IN (c,d)
        Map<String, List<String>> bySite = new LinkedHashMap<>();
        for (String key : keys) {
            String[] parts = key.split("\\|", 2);
            if (parts.length == 2 && StringUtils.hasText(parts[0]) && StringUtils.hasText(parts[1])) {
                bySite.computeIfAbsent(parts[0], k -> new ArrayList<>()).add(parts[1]);
            }
        }
        if (bySite.isEmpty()) return Collections.emptyList();
        LambdaQueryWrapper<EbayProductDedupEntity> wrapper = new LambdaQueryWrapper<>();
        boolean first = true;
        for (Map.Entry<String, List<String>> entry : bySite.entrySet()) {
            if (first) {
                wrapper.and(w -> w.eq(EbayProductDedupEntity::getSite, entry.getKey())
                        .in(EbayProductDedupEntity::getSku, entry.getValue()));
                first = false;
            } else {
                wrapper.or(w -> w.eq(EbayProductDedupEntity::getSite, entry.getKey())
                        .in(EbayProductDedupEntity::getSku, entry.getValue()));
            }
        }
        return mapper.selectList(wrapper);
    }

    @Override
    public void saveTrackingCalc(String site, String sku, java.math.BigDecimal trackingPrice,
                                  java.math.BigDecimal profitMargin, java.math.BigDecimal floorPrice) {
        EbayProductDedupEntity e = mapper.selectOne(
                new LambdaQueryWrapper<EbayProductDedupEntity>()
                        .eq(EbayProductDedupEntity::getSite, site)
                        .eq(EbayProductDedupEntity::getSku, sku));
        if (e != null) {
            e.setTrackingPrice(trackingPrice);
            e.setTrackingProfitMargin(profitMargin);
            e.setFloorPrice(floorPrice);
            mapper.updateById(e);
        }
    }

    @Override
    public Map<String, java.math.BigDecimal> batchGetTrackingProfitMargins() {
        Map<String, java.math.BigDecimal> result = new LinkedHashMap<>();
        for (EbayProductDedupEntity e : mapper.selectList(null)) {
            if (StringUtils.hasText(e.getSite()) && StringUtils.hasText(e.getSku()) && e.getTrackingProfitMargin() != null) {
                result.put(e.getSite() + "|" + e.getSku(), e.getTrackingProfitMargin());
            }
        }
        return result;
    }

    @Override
    public Map<String, java.math.BigDecimal> batchGetFloorPrices() {
        Map<String, java.math.BigDecimal> result = new LinkedHashMap<>();
        for (EbayProductDedupEntity e : mapper.selectList(null)) {
            if (StringUtils.hasText(e.getSite()) && StringUtils.hasText(e.getSku()) && e.getFloorPrice() != null) {
                result.put(e.getSite() + "|" + e.getSku(), e.getFloorPrice());
            }
        }
        return result;
    }

    @Override
    public Map<String, java.math.BigDecimal> batchGetTrackingPrices() {
        Map<String, java.math.BigDecimal> result = new LinkedHashMap<>();
        for (EbayProductDedupEntity e : mapper.selectList(null)) {
            if (StringUtils.hasText(e.getSite()) && StringUtils.hasText(e.getSku()) && e.getTrackingPrice() != null) {
                result.put(e.getSite() + "|" + e.getSku(), e.getTrackingPrice());
            }
        }
        return result;
    }

    private static final Map<String, String> SHEET_TO_SITE = new LinkedHashMap<>();
    static {
        SHEET_TO_SITE.put("US", "美国");
        SHEET_TO_SITE.put("UK", "英国");
        SHEET_TO_SITE.put("DE", "德国");
    }

    @Override
    public Map<String, Object> importProfitRate(byte[] fileBytes) {
        Map<String, Map<String, java.math.BigDecimal>> allRates = new LinkedHashMap<>();
        int totalRows = 0, parseSkipped = 0;
        List<Map<String, Object>> skipDetails = new ArrayList<>();
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(fileBytes))) {
            DataFormatter df = new DataFormatter();
            for (int s = 0; s < wb.getNumberOfSheets(); s++) {
                Sheet sheet = wb.getSheetAt(s);
                String sheetName = sheet.getSheetName().trim();
                String site = SHEET_TO_SITE.get(sheetName);
                if (site == null) site = SHEET_TO_SITE.get(sheetName.toUpperCase());
                if (site == null) {
                    Map<String,Object> d=new LinkedHashMap<>(); d.put("sheet",sheetName); d.put("reason","未知站点"); skipDetails.add(d); continue;
                }
                Row hr = sheet.getRow(0); if (hr == null) continue;
                int colSku = -1, colRate = -1;
                for (int c = 0; c < hr.getLastCellNum(); c++) {
                    Cell cell = hr.getCell(c); if (cell == null) continue;
                    String h = df.formatCellValue(cell).trim();
                    if (h.equalsIgnoreCase("SKU") || h.contains("产品代码")) colSku = c;
                    else if (h.equalsIgnoreCase("Profit") || h.contains("利润率")) colRate = c;
                }
                if (colSku < 0) colSku = 0; if (colRate < 0) colRate = 1;
                for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                    Row row = sheet.getRow(r); if (row == null) continue;
                    String fullSku = df.formatCellValue(row.getCell(colSku)).trim();
                    String rateStr = df.formatCellValue(row.getCell(colRate)).trim();
                    if (fullSku.isEmpty() || rateStr.isEmpty()) {
                        Map<String,Object> d=new LinkedHashMap<>(); d.put("row",r+1); d.put("sheet",sheetName); d.put("sku",fullSku); d.put("rate",rateStr); d.put("reason","SKU或利润率为空"); skipDetails.add(d); parseSkipped++; continue;
                    }
                    if (rateStr.endsWith("%")) {
                        try { rateStr = new java.math.BigDecimal(rateStr.replace("%","").trim()).divide(new java.math.BigDecimal("100"),6,java.math.RoundingMode.HALF_UP).toString(); } catch(Exception ignored){}
                    }
                    String mid = InventoryUtils.extractMiddleCodeForInventory(fullSku);
                    if (mid.isEmpty()) mid = fullSku;
                    try { allRates.computeIfAbsent(mid, k->new LinkedHashMap<>()).put(site, new java.math.BigDecimal(rateStr)); totalRows++; }
                    catch (Exception ignored) {
                        Map<String,Object> d=new LinkedHashMap<>(); d.put("row",r+1); d.put("sheet",sheetName); d.put("mid",mid); d.put("rate",rateStr); d.put("reason","利润率数值格式异常"); skipDetails.add(d); parseSkipped++;
                    }
                }
            }
        } catch (Exception e) { throw new RuntimeException("解析Excel失败: " + e.getMessage()); }

        Map<String, EbayProductDedupEntity> dedupByKey = new LinkedHashMap<>();
        for (EbayProductDedupEntity ent : mapper.selectList(null)) {
            if (ent.getSku() == null || ent.getSku().isEmpty() || ent.getSite() == null) continue;
            String mid = InventoryUtils.extractMiddleCodeForInventory(ent.getSku());
            if (!mid.isEmpty()) dedupByKey.put(ent.getSite() + "|" + mid, ent);
        }
        int updated = 0, skipped = 0;
        for (Map.Entry<String, Map<String, java.math.BigDecimal>> entry : allRates.entrySet()) {
            String mid = entry.getKey();
            for (Map.Entry<String, java.math.BigDecimal> se : entry.getValue().entrySet()) {
                EbayProductDedupEntity ent = dedupByKey.get(se.getKey() + "|" + mid);
                if (ent != null) { ent.setProfitRate(se.getValue()); mapper.updateById(ent); updated++; }
                else {
                    Map<String,Object> d=new LinkedHashMap<>(); d.put("middleCode",mid); d.put("site",se.getKey()); d.put("rate",se.getValue()); d.put("reason","在ebay_product_dedup中未匹配到(site+中间码)"); skipDetails.add(d); skipped++;
                }
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", totalRows); result.put("updated", updated); result.put("skipped", skipped + parseSkipped);
        result.put("skipDetails", skipDetails);
        return result;
    }

    @Override
    public Map<String, java.math.BigDecimal> batchGetLowestPrices() {
        Map<String, java.math.BigDecimal> result = new LinkedHashMap<>();
        for (EbayProductDedupEntity e : mapper.selectList(null)) {
            if (StringUtils.hasText(e.getSite()) && StringUtils.hasText(e.getSku())
                    && e.getLowestPrice() != null) {
                result.put(e.getSite() + "|" + e.getSku(), e.getLowestPrice());
            }
        }
        return result;
    }

    @Override
    public Map<String, Object> importLowestPrice(byte[] fileBytes, String fileName) {
        int inserted = 0, updated = 0, skipped = 0;
        List<Map<String, Object>> skipDetails = new ArrayList<>();

        // 1. 解析 Excel → 按 (site, sku) 取最低价
        Map<String, LowestPriceExcelRow> lowestByKey = new LinkedHashMap<>();
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(fileBytes))) {
            Sheet sheet = wb.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) throw new IllegalArgumentException("Excel 文件无表头");

            int colSku = -1, colSite = -1, colPrice = -1, colItemNo = -1;
            DataFormatter df = new DataFormatter();
            for (int c = 0; c < headerRow.getLastCellNum(); c++) {
                Cell cell = headerRow.getCell(c);
                if (cell == null) continue;
                String h = df.formatCellValue(cell).trim();
                if ("SKU".equalsIgnoreCase(h)) colSku = c;
                else if ("站点".equals(h) || "Site".equalsIgnoreCase(h)) colSite = c;
                else if ("价格".equals(h) || "Price".equalsIgnoreCase(h)) colPrice = c;
                else if ("Item Number".equalsIgnoreCase(h)) colItemNo = c;
            }
            if (colSku < 0) colSku = 0;
            if (colSite < 0) colSite = 1;
            if (colPrice < 0) colPrice = 2;

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                int rowNum = r + 1;
                String rawSku = df.formatCellValue(row.getCell(colSku)).trim();
                String rawSite = df.formatCellValue(row.getCell(colSite)).trim();
                String itemNo = colItemNo >= 0 ? df.formatCellValue(row.getCell(colItemNo)).trim() : "";
                String priceStr = df.formatCellValue(row.getCell(colPrice)).trim();

                if (rawSku.isEmpty() || rawSite.isEmpty() || priceStr.isEmpty()) {
                    skipDetails.add(buildSkip(rowNum, rawSku, rawSite, "SKU/站点/价格为空")); skipped++; continue;
                }
                java.math.BigDecimal price;
                try { price = new java.math.BigDecimal(priceStr); } catch (Exception e) {
                    skipDetails.add(buildSkip(rowNum, rawSku, rawSite, "价格格式异常: " + priceStr)); skipped++; continue;
                }

                String sku = InventoryUtils.extractBaseSku(rawSku).trim();
                String site = InventoryUtils.mapSiteName(rawSite).trim();
                if (sku.isEmpty() || site.isEmpty()) {
                    skipDetails.add(buildSkip(rowNum, rawSku, rawSite, "SKU/站点无法识别")); skipped++; continue;
                }

                String key = site + "|" + sku;
                LowestPriceExcelRow exist = lowestByKey.get(key);
                if (exist == null || price.compareTo(exist.price) < 0) {
                    lowestByKey.put(key, new LowestPriceExcelRow(sku, site, itemNo, price));
                }
            }
        } catch (Exception e) {
            LOG.error("解析最低价Excel失败: {}", fileName, e);
            throw new RuntimeException("解析Excel失败: " + e.getMessage());
        }

        // 2. 加载已有记录（trim 防止空格不匹配）
        Map<String, EbayProductDedupEntity> existingMap = new LinkedHashMap<>();
        for (EbayProductDedupEntity ent : mapper.selectList(null)) {
            if (StringUtils.hasText(ent.getSite()) && StringUtils.hasText(ent.getSku())) {
                existingMap.put(ent.getSite().trim() + "|" + ent.getSku().trim(), ent);
            }
        }

        // 3. 增量 upsert
        for (Map.Entry<String, LowestPriceExcelRow> entry : lowestByKey.entrySet()) {
            LowestPriceExcelRow row = entry.getValue();
            EbayProductDedupEntity existing = existingMap.get(entry.getKey());
            if (existing != null) {
                if (existing.getLowestPrice() == null || row.price.compareTo(existing.getLowestPrice()) < 0) {
                    existing.setLowestPrice(row.price);
                    existing.setLowestItemNumber(row.itemNumber);
                    existing.setLowestUploadTime(java.time.LocalDateTime.now());
                    mapper.updateById(existing);
                    updated++;
                } else {
                    skipDetails.add(buildSkip(null, row.sku, row.site,
                            "价格未更低: " + row.price + " >= " + existing.getLowestPrice())); skipped++;
                }
            } else {
                EbayProductDedupEntity entity = new EbayProductDedupEntity();
                entity.setSite(row.site); entity.setSku(row.sku);
                entity.setLowestPrice(row.price);
                entity.setLowestItemNumber(row.itemNumber);
                entity.setLowestUploadTime(java.time.LocalDateTime.now());
                mapper.insert(entity);
                inserted++;
            }
        }

        LOG.info("最低价导入完成: {} 条, 新增{} 更新{} 跳过{}", fileName, inserted, updated, skipped);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", lowestByKey.size());
        result.put("inserted", inserted);
        result.put("updated", updated);
        result.put("skipped", skipped);
        result.put("skipDetails", skipDetails);
        return result;
    }

    private Map<String, Object> buildSkip(Integer rowNum, String sku, String site, String reason) {
        Map<String, Object> d = new LinkedHashMap<>();
        if (rowNum != null) d.put("row", rowNum);
        d.put("sku", sku); d.put("site", site); d.put("reason", reason);
        return d;
    }

    private static class LowestPriceExcelRow {
        String sku, site, itemNumber;
        java.math.BigDecimal price;
        LowestPriceExcelRow(String s, String t, String i, java.math.BigDecimal p) { sku = s; site = t; itemNumber = i; price = p; }
    }

    @Override
    public List<EbayProductDedupEntity> listAll() {
        return mapper.selectList(null);
    }

    @Override
    public Map<String, Object> importReturnRate(byte[] fileBytes) {
        Map<String, java.math.BigDecimal> rateMap = new LinkedHashMap<>();
        List<Map<String, Object>> skipDetails = new ArrayList<>();
        int parseSkipped = 0;
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(fileBytes))) {
            Sheet sheet = wb.getSheetAt(0); Row hr = sheet.getRow(0);
            int colSku = -1, colRate = -1;
            DataFormatter df = new DataFormatter();
            for (int c = 0; c < hr.getLastCellNum(); c++) {
                Cell cell = hr.getCell(c); if (cell == null) continue;
                String h = df.formatCellValue(cell).trim();
                if (h.contains("SKU") || h.contains("产品SKU")) colSku = c;
                else if (h.contains("各平台售后率")) colRate = c;
            }
            if (colSku < 0) colSku = 0; if (colRate < 0) colRate = 4;
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r); if (row == null) continue;
                String fullSku = df.formatCellValue(row.getCell(colSku)).trim();
                String rateStr = df.formatCellValue(row.getCell(colRate)).trim();
                if (fullSku.isEmpty() || rateStr.isEmpty()) {
                    Map<String,Object> d=new LinkedHashMap<>(); d.put("row",r+1); d.put("sku",fullSku); d.put("rateStr",rateStr); d.put("reason","SKU或退货率为空"); skipDetails.add(d); parseSkipped++; continue;
                }
                if (rateStr.endsWith("%")) {
                    try { rateStr = new java.math.BigDecimal(rateStr.replace("%","").trim()).divide(new java.math.BigDecimal("100"),6,java.math.RoundingMode.HALF_UP).toString(); } catch(Exception ignored){}
                }
                String mid = InventoryUtils.extractMiddleCodeForInventory(fullSku);
                if (mid.isEmpty()) mid = fullSku;
                try { rateMap.putIfAbsent(mid, new java.math.BigDecimal(rateStr)); } catch (Exception ignored) {
                    Map<String,Object> d=new LinkedHashMap<>(); d.put("row",r+1); d.put("mid",mid); d.put("rateStr",rateStr); d.put("reason","退货率格式异常"); skipDetails.add(d); parseSkipped++;
                }
            }
        } catch (Exception e) { throw new RuntimeException("解析Excel失败: " + e.getMessage()); }
        int updated = 0, skipped = 0;
        Map<String, EbayProductDedupEntity> dedupByMid = new LinkedHashMap<>();
        for (EbayProductDedupEntity ent : mapper.selectList(null)) {
            if (ent.getSku() == null || ent.getSku().isEmpty()) continue;
            String mid = InventoryUtils.extractMiddleCodeForInventory(ent.getSku());
            if (!mid.isEmpty()) dedupByMid.putIfAbsent(mid, ent);
        }
        for (Map.Entry<String, java.math.BigDecimal> e : rateMap.entrySet()) {
            EbayProductDedupEntity ent = dedupByMid.get(e.getKey());
            if (ent != null) { ent.setReturnRate(e.getValue()); mapper.updateById(ent); updated++; }
            else {
                Map<String,Object> d=new LinkedHashMap<>(); d.put("middleCode",e.getKey()); d.put("rate",e.getValue()); d.put("reason","中间码在ebay_product_dedup中未匹配到"); skipDetails.add(d); skipped++;
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", rateMap.size()); result.put("updated", updated); result.put("skipped", skipped + parseSkipped);
        result.put("skipDetails", skipDetails);
        return result;
    }
}
