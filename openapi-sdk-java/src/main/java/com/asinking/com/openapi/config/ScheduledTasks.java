package com.asinking.com.openapi.config;

import com.asinking.com.openapi.dto.request.WarehouseSyncRequest;
import com.asinking.com.openapi.dto.response.SaleStatSyncResponse;
import com.asinking.com.openapi.dto.response.WarehouseInventoryDetailSyncResponse;
import com.asinking.com.openapi.dto.response.WarehouseSyncResponse;
import com.asinking.com.openapi.service.GoodcangSyncService;
import com.asinking.com.openapi.service.LingxingWarehouseInventoryService;
import com.asinking.com.openapi.service.LingxingWarehouseService;
import com.asinking.com.openapi.service.LingxingPurchaseOrderService;
import com.asinking.com.openapi.service.LingxingEbayService;
import com.asinking.com.openapi.service.LingxingPurchasePlanQueryService;
import com.asinking.com.openapi.service.LingxingWarehouseStatementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledTasks {

    private static final Logger LOG = LoggerFactory.getLogger(ScheduledTasks.class);
    private final LingxingWarehouseService warehouseService;
    private final LingxingWarehouseInventoryService inventoryService;
    private final GoodcangSyncService goodcangSyncService;
    private final LingxingWarehouseStatementService statementService;
    private final LingxingPurchaseOrderService purchaseOrderService;
    private final LingxingEbayService ebayService;
    private final LingxingPurchasePlanQueryService purchasePlanQueryService;

    public ScheduledTasks(LingxingWarehouseService warehouseService,
                          LingxingWarehouseInventoryService inventoryService,
                          GoodcangSyncService goodcangSyncService,
                          LingxingWarehouseStatementService statementService,
                          LingxingPurchaseOrderService purchaseOrderService,
                          LingxingEbayService ebayService,
                          LingxingPurchasePlanQueryService purchasePlanQueryService) {
        this.warehouseService = warehouseService;
        this.inventoryService = inventoryService;
        this.goodcangSyncService = goodcangSyncService;
        this.statementService = statementService;
        this.purchaseOrderService = purchaseOrderService;
        this.ebayService = ebayService;
        this.purchasePlanQueryService = purchasePlanQueryService;
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void syncWarehouses() {
        LOG.info("==== 仓库同步 开始 ====");
        try { long t = System.currentTimeMillis();
            WarehouseSyncResponse r = warehouseService.syncOverseaWarehouses(new WarehouseSyncRequest());
            LOG.info("[仓库] 新增{} 更新{} 耗时{}s", r.getInserted(), r.getUpdated(), (System.currentTimeMillis()-t)/1000);
        } catch (Exception e) { LOG.error("[仓库] 失败", e); }
    }

    @Scheduled(cron = "0 5 0 * * ?")
    public void syncInventory() {
        LOG.info("==== 库存同步 开始 ====");
        try { long t = System.currentTimeMillis();
            WarehouseInventoryDetailSyncResponse r = inventoryService.syncAllInventoryDetails(null);
            LOG.info("[库存] 新增{} 耗时{}s", r.getInserted(), (System.currentTimeMillis()-t)/1000);
        } catch (Exception e) { LOG.error("[库存] 失败", e); }
    }

    @Scheduled(cron = "0 25 0 * * ?")
    public void syncGoodcangGrn() {
        LOG.info("==== 谷仓入库单同步 开始 ====");
        try { long t = System.currentTimeMillis();
            String to = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            SaleStatSyncResponse r = goodcangSyncService.syncGrn("2026-01-01 00:00:00", to);
            LOG.info("[谷仓入库单] 单数:{} 耗时{}s", r.getInserted(), (System.currentTimeMillis()-t)/1000);
        } catch (Exception e) { LOG.error("[谷仓入库单] 失败", e); }
    }

    @Scheduled(cron = "0 30 0 * * ?")
    public void syncWarehouseStatement() {
        LOG.info("==== 仓库库存流水同步 开始 ====");
        try { long t = System.currentTimeMillis();
            LocalDate now = LocalDate.now();
            SaleStatSyncResponse r = statementService.syncStatements(now.minusDays(90).toString(), now.toString());
            LOG.info("[库存流水] 处理:{} 耗时{}s", r.getInserted(), (System.currentTimeMillis()-t)/1000);
        } catch (Exception e) { LOG.error("[库存流水] 失败", e); }
    }

    @Scheduled(cron = "0 35 0 * * ?")
    public void syncPurchaseOrder() {
        LOG.info("==== 采购单同步 开始 ====");
        try { long t = System.currentTimeMillis();
            LocalDate now = LocalDate.now();
            SaleStatSyncResponse r = purchaseOrderService.sync(now.minusDays(1).toString(), now.minusDays(1).toString());
            LOG.info("[采购单] 处理:{} 耗时{}s", r.getInserted(), (System.currentTimeMillis()-t)/1000);
        } catch (Exception e) { LOG.error("[采购单] 失败", e); }
    }

    @Scheduled(cron = "0 3 0 * * ?")
    public void syncEbayItems() {
        LOG.info("==== eBay商品同步 开始 ====");
        try { long t = System.currentTimeMillis();
            var r = ebayService.syncAllEbayItems(null);
            LOG.info("[eBay商品] 处理:{} 耗时{}s", r.getRemoteTotal(), (System.currentTimeMillis()-t)/1000);
        } catch (Exception e) { LOG.error("[eBay商品] 失败", e); }
    }

    @Scheduled(cron = "0 20 0 * * ?")
    public void syncGoodcangWarehouses() {
        LOG.info("==== 谷仓仓库同步 开始 ====");
        try { long t = System.currentTimeMillis();
            SaleStatSyncResponse r = goodcangSyncService.syncWarehouses();
            LOG.info("[谷仓仓库] 处理:{} 耗时{}s", r.getInserted(), (System.currentTimeMillis()-t)/1000);
        } catch (Exception e) { LOG.error("[谷仓仓库] 失败", e); }
    }

    @Scheduled(cron = "0 40 0 * * ?")
    public void syncPurchasePlan() {
        LOG.info("==== 采购计划同步 开始 ====");
        try { long t = System.currentTimeMillis();
            LocalDate now = LocalDate.now();
            SaleStatSyncResponse r = purchasePlanQueryService.sync(now.minusDays(1).toString(), now.minusDays(1).toString());
            LOG.info("[采购计划] 处理:{} 耗时{}s", r.getInserted(), (System.currentTimeMillis()-t)/1000);
        } catch (Exception e) { LOG.error("[采购计划] 失败", e); }
    }
}
