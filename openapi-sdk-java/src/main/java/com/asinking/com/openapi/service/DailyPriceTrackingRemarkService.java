package com.asinking.com.openapi.service;

import com.asinking.com.openapi.entity.DailyPriceTrackingRemarkEntity;
import java.util.List;
import java.util.Map;

/**
 * 每日跟价备注服务：按 (site, sku) 保存/更新/查询备注。
 */
public interface DailyPriceTrackingRemarkService {

    /**
     * 保存或更新备注（按 site+sku 唯一键 upsert）。
     * @return 保存后的实体
     */
    DailyPriceTrackingRemarkEntity saveOrUpdate(String site, String sku, String remark);

    /**
     * 批量查询备注，返回 (site + "|" + sku) → remark 的映射。
     */
    Map<String, String> batchGetRemarks(List<String> keys);

    /**
     * 查询所有备注。
     */
    List<DailyPriceTrackingRemarkEntity> listAll();
}
