package com.asinking.com.openapi.config;

import com.asinking.com.openapi.service.DailyPriceTrackingService;
import com.asinking.com.openapi.service.InventoryOverviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 启动缓存预热：若补货/跟价缓存表为空，异步触发一次重算。
 * 不阻塞应用启动，不等待完成，用户访问时已有数据。
 */
@Component
public class CacheWarmup {

    private static final Logger LOG = LoggerFactory.getLogger(CacheWarmup.class);
    private final InventoryOverviewService overviewService;
    private final DailyPriceTrackingService trackingService;

    public CacheWarmup(InventoryOverviewService overviewService,
                       DailyPriceTrackingService trackingService) {
        this.overviewService = overviewService;
        this.trackingService = trackingService;
    }

    @Async("taskExecutor")
    @EventListener(ApplicationReadyEvent.class)
    public void warmup() {
        LOG.info("==== 缓存预热开始 ====");
        try {
            // 补货快照：仅当缓存为空时触发
            if (overviewService.buildOverview().isEmpty()) {
                LOG.info("[预热] 补货缓存为空，触发重算...");
                overviewService.refreshSnapshot();
                LOG.info("[预热] 补货缓存重算完成");
            } else {
                LOG.info("[预热] 补货缓存非空，跳过");
            }
        } catch (Exception e) {
            LOG.error("[预热] 补货缓存失败", e);
        }
        try {
            // 跟价缓存：仅当缓存为空时触发（避免每次重启全量重算）
            if (trackingService.page(1, 1, null, null, null, null, null, null).getTotal() == 0) {
                LOG.info("[预热] 跟价缓存为空，触发重算...");
                trackingService.refreshTable();
                LOG.info("[预热] 跟价缓存重算完成");
            } else {
                LOG.info("[预热] 跟价缓存非空，跳过");
            }
        } catch (Exception e) {
            LOG.error("[预热] 跟价缓存失败", e);
        }
        LOG.info("==== 缓存预热结束 ====");
    }
}
