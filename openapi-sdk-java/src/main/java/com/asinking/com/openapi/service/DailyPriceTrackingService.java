package com.asinking.com.openapi.service;

import com.asinking.com.openapi.common.response.PageResult;
import com.asinking.com.openapi.entity.DailyPriceTrackingEntity;

/**
 * 每日跟价服务：数据来源于运营总览（补货页），实时映射。
 */
public interface DailyPriceTrackingService {

    /** 分页查询，支持站点、SKU、品牌（负责人）、操作员筛选 */
    PageResult<DailyPriceTrackingEntity> page(long page, long size, String site, String sku, String brand, String operator);
}
