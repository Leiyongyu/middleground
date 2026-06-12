package com.asinking.com.openapi.config;

import com.asinking.com.openapi.dto.request.WarehouseSyncRequest;
import com.asinking.com.openapi.dto.response.SaleStatSyncResponse;
import com.asinking.com.openapi.dto.response.WarehouseInventoryDetailSyncResponse;
import com.asinking.com.openapi.dto.response.WarehouseSyncResponse;
import com.asinking.com.openapi.service.GoodcangProductService;
import com.asinking.com.openapi.service.GoodcangSyncService;
import com.asinking.com.openapi.service.LingxingWarehouseInventoryService;
import com.asinking.com.openapi.service.LingxingWarehouseService;
import com.asinking.com.openapi.service.LingxingEbayService;
import com.asinking.com.openapi.service.LingxingPurchaseOrderService;
import com.asinking.com.openapi.service.LingxingPurchasePlanQueryService;
import com.asinking.com.openapi.service.LingxingWarehouseStatementService;
import com.asinking.com.openapi.service.InventoryOverviewService;
import com.asinking.com.openapi.service.OperationLogService;
import com.asinking.com.openapi.service.DailyPriceTrackingService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 定时任务：每日凌晨 00:00~00:50 依次同步各外部数据源，完成后刷新快照。
 *  使用 Redis 分布式锁防止多实例重复执行。 */
@Component
public class ScheduledTasks {

    private static final Logger LOG = LoggerFactory.getLogger(ScheduledTasks.class);
    private final LingxingWarehouseService warehouseService;
    private final LingxingEbayService ebayService;
    private final LingxingWarehouseInventoryService inventoryService;
    private final GoodcangSyncService goodcangSyncService;
    private final LingxingWarehouseStatementService statementService;
    private final LingxingPurchaseOrderService purchaseOrderService;
    private final LingxingPurchasePlanQueryService purchasePlanQueryService;
    private final InventoryOverviewService overviewService;
    private final DailyPriceTrackingService trackingService;
    private final GoodcangProductService goodcangProductService;
    private final OperationLogService logService;
    private final RateLimitInterceptor rateLimitInterceptor;
    private final RedissonClient redissonClient;

    public ScheduledTasks(LingxingWarehouseService warehouseService,
                          LingxingEbayService ebayService,
                          LingxingWarehouseInventoryService inventoryService,
                          GoodcangSyncService goodcangSyncService,
                          LingxingWarehouseStatementService statementService,
                          LingxingPurchaseOrderService purchaseOrderService,
                          LingxingPurchasePlanQueryService purchasePlanQueryService,
                          InventoryOverviewService overviewService,
                          DailyPriceTrackingService trackingService,
                          GoodcangProductService goodcangProductService,
                          OperationLogService logService,
                          RateLimitInterceptor rateLimitInterceptor,
                          RedissonClient redissonClient) {
        this.warehouseService = warehouseService;
        this.ebayService = ebayService;
        this.inventoryService = inventoryService;
        this.goodcangSyncService = goodcangSyncService;
        this.statementService = statementService;
        this.purchaseOrderService = purchaseOrderService;
        this.purchasePlanQueryService = purchasePlanQueryService;
        this.overviewService = overviewService;
        this.trackingService = trackingService;
        this.goodcangProductService = goodcangProductService;
        this.logService = logService;
        this.rateLimitInterceptor = rateLimitInterceptor;
        this.redissonClient = redissonClient;
    }

    @Scheduled(cron = "0 3 0 * * ?")
    public void syncWarehouse() { locked("warehouse", () ->
        step("同步外服", "领星-仓库信息", () -> {
            WarehouseSyncResponse r = warehouseService.syncOverseaWarehouses(new WarehouseSyncRequest());
            return new int[]{r.getInserted(), r.getUpdated(), 0, r.getInserted()+r.getUpdated()};
        }));
    }

    @Scheduled(cron = "0 5 0 * * ?")
    public void syncInventory() { locked("inventory", () ->
        step("同步外服", "领星-库存明细", () -> {
            WarehouseInventoryDetailSyncResponse r = inventoryService.syncAllInventoryDetails(null);
            return new int[]{r.getInserted(), 0, 0, r.getInserted()};
        }));
    }

    @Scheduled(cron = "0 8 0 * * ?")
    public void syncEbayItems() { locked("ebay-items", () ->
        step("同步外服", "领星-eBay商品刊登", () -> {
            var r = ebayService.syncAllEbayItems(null);
            int t = r.getRemoteTotal();
            return new int[]{t, 0, 0, t};
        }));
    }

    @Scheduled(cron = "0 12 0 * * ?")
    public void syncGoodcangWarehouses() { locked("gc-warehouse", () ->
        step("同步外服", "谷仓-仓库信息", () -> {
            var r = goodcangSyncService.syncWarehouses();
            return new int[]{r.getInserted(), 0, 0, r.getInserted()};
        }));
    }

    @Scheduled(cron = "0 25 0 * * ?")
    public void syncGoodcangGrn() { locked("gc-grn", () ->
        step("同步外服", "谷仓-入库单", () -> {
            String to = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String from = java.time.LocalDateTime.now().minusDays(2).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            SaleStatSyncResponse r = goodcangSyncService.syncGrn(from, to);
            return new int[]{r.getInserted(), 0, 0, r.getInserted()};
        }));
    }

    @Scheduled(cron = "0 28 0 * * ?")
    public void syncGoodcangGrnDetail() { locked("gc-grn-detail", () ->
        step("同步外服", "谷仓-入库单详情", () -> {
            SaleStatSyncResponse r = goodcangSyncService.syncAllGrnDetails();
            return new int[]{r.getInserted(), 0, 0, r.getInserted()};
        }));
    }

    @Scheduled(cron = "0 30 0 * * ?")
    public void syncWarehouseStatement() { locked("statement", () ->
        step("同步外服", "领星-库存流水", () -> {
            LocalDate now = LocalDate.now();
            SaleStatSyncResponse r = statementService.syncStatements(now.minusDays(2).toString(), now.toString());
            return new int[]{r.getInserted(), 0, 0, r.getInserted()};
        }));
    }

    @Scheduled(cron = "0 35 0 * * ?")
    public void syncPurchaseOrder() { locked("purchase-order", () ->
        step("同步外服", "领星-采购单", () -> {
            LocalDate now = LocalDate.now();
            SaleStatSyncResponse r = purchaseOrderService.sync(now.minusDays(1).toString(), now.minusDays(1).toString());
            return new int[]{r.getInserted(), 0, 0, r.getInserted()};
        }));
    }

    @Scheduled(cron = "0 40 0 * * ?")
    public void syncPurchasePlan() { locked("purchase-plan", () ->
        step("同步外服", "领星-采购计划", () -> {
            LocalDate now = LocalDate.now();
            SaleStatSyncResponse r = purchasePlanQueryService.sync(now.minusDays(1).toString(), now.minusDays(1).toString());
            return new int[]{r.getInserted(), 0, 0, r.getInserted()};
        }));
    }

    @Scheduled(cron = "0 45 0 * * ?")
    public void refreshInventorySnapshot() { locked("refresh-overview", () -> {
        overviewService.refreshSnapshot();
    });}

    @Scheduled(cron = "0 47 0 * * ?")
    public void refreshTrackingTable() { locked("refresh-tracking", () -> {
        trackingService.refreshTable();
    });}

    @Scheduled(cron = "0 55 0 * * ?")
    public void evictRateLimitBuckets() { locked("evict-rate-limit", () -> {
        rateLimitInterceptor.evictStale();
    });}

    @Scheduled(cron = "0 10 0 * * 1")
    public void syncGoodcangProducts() { locked("gc-products", () ->
        step("同步外服", "谷仓-商品信息", () -> {
            Map<String, Object> r = goodcangProductService.syncFromApi();
            int inserted = ((Number)r.getOrDefault("inserted",0)).intValue();
            int updated = ((Number)r.getOrDefault("updated",0)).intValue();
            return new int[]{inserted, updated, 0, ((Number)r.getOrDefault("total",0)).intValue()};
        }));
    }

    // ==== 辅助 ====

    @FunctionalInterface
    private interface Step { int[] run() throws Exception; }

    /** 带分布式锁执行：获取锁失败（其他实例正在执行）则静默跳过 */
    private void locked(String name, Runnable task) {
        String lockKey = "scheduled:" + name;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (!lock.tryLock(0, 30, TimeUnit.MINUTES)) {
                LOG.info("[跳过] {} — 其他实例正在执行", name);
                return;
            }
            task.run();
        } catch (Exception e) {
            LOG.error("[{}] 执行异常", name, e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                try { lock.unlock(); } catch (Exception ignored) {}
            }
        }
    }

    private void step(String type, String target, Step s) {
        LOG.info("==== {} 开始 ====", target);
        try { long t = System.currentTimeMillis();
            int[] r = s.run();
            int inserted = r[0], updated = r[1], skipped = r[2], total = r[3];
            logService.log(type, target, "成功", total, inserted + updated, skipped, null);
            LOG.info("[{}] 新增{} 更新{} 耗时{}s", target, inserted, updated, (System.currentTimeMillis()-t)/1000);
        } catch (Exception e) {
            logService.log(type, target, "失败", 0, 0, 0, e.getMessage());
            LOG.error("[{}] 失败", target, e);
        }
    }

}
