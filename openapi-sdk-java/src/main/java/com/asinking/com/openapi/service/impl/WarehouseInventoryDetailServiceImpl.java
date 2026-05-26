package com.asinking.com.openapi.service.impl;

import com.asinking.com.openapi.entity.WarehouseInventoryDetailEntity;
import com.asinking.com.openapi.mapper.mp.WarehouseInventoryDetailMapper;
import com.asinking.com.openapi.service.WarehouseInventoryDetailService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 库存明细业务实现，基于 MyBatis-Plus IService 提供标准 CRUD。
 */
@Service
public class WarehouseInventoryDetailServiceImpl extends ServiceImpl<WarehouseInventoryDetailMapper, WarehouseInventoryDetailEntity> implements WarehouseInventoryDetailService {
}
