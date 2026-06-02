package com.asinking.com.openapi.service;

import com.asinking.com.openapi.entity.WarehouseEntity;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 仓库业务接口，管理 warehouse 表。
 */
public interface WarehouseService extends IService<WarehouseEntity> {

    /** 根据 wid 查询仓库 */
    WarehouseEntity getByWid(Integer wid);

    /** 根据 wid 列表批量查询仓库 */
    java.util.List<WarehouseEntity> listByWids(java.util.List<Integer> wids);
}
