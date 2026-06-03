package com.asinking.com.openapi.service.impl;

import com.asinking.com.openapi.entity.DailyPriceTrackingRemarkEntity;
import com.asinking.com.openapi.mapper.mp.DailyPriceTrackingRemarkMapper;
import com.asinking.com.openapi.service.DailyPriceTrackingRemarkService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DailyPriceTrackingRemarkServiceImpl implements DailyPriceTrackingRemarkService {

    private final DailyPriceTrackingRemarkMapper mapper;

    public DailyPriceTrackingRemarkServiceImpl(DailyPriceTrackingRemarkMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public DailyPriceTrackingRemarkEntity saveOrUpdate(String site, String sku, String remark) {
        // 查询是否已存在
        DailyPriceTrackingRemarkEntity existing = mapper.selectOne(
                new LambdaQueryWrapper<DailyPriceTrackingRemarkEntity>()
                        .eq(DailyPriceTrackingRemarkEntity::getSite, site)
                        .eq(DailyPriceTrackingRemarkEntity::getSku, sku));

        if (existing != null) {
            existing.setRemark(remark);
            mapper.updateById(existing);
            return existing;
        } else {
            DailyPriceTrackingRemarkEntity entity = new DailyPriceTrackingRemarkEntity();
            entity.setSite(site);
            entity.setSku(sku);
            entity.setRemark(remark);
            mapper.insert(entity);
            return entity;
        }
    }

    @Override
    public Map<String, String> batchGetRemarks(List<String> keys) {
        if (keys == null || keys.isEmpty()) return Collections.emptyMap();
        Map<String, String> result = new LinkedHashMap<>();
        // keys 格式: "site|sku"，批量加载所有备注后内存匹配
        List<DailyPriceTrackingRemarkEntity> all = mapper.selectList(null);
        for (DailyPriceTrackingRemarkEntity e : all) {
            if (StringUtils.hasText(e.getSite()) && StringUtils.hasText(e.getSku())) {
                result.put(e.getSite() + "|" + e.getSku(),
                        StringUtils.hasText(e.getRemark()) ? e.getRemark() : "");
            }
        }
        return result;
    }

    @Override
    public List<DailyPriceTrackingRemarkEntity> listAll() {
        return mapper.selectList(null);
    }
}
