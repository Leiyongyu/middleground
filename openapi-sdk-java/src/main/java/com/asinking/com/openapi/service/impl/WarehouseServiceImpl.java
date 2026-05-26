package com.asinking.com.openapi.service.impl;

import com.asinking.com.openapi.entity.WarehouseEntity;
import com.asinking.com.openapi.mapper.mp.WarehouseMapper;
import com.asinking.com.openapi.service.WarehouseService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 仓库业务实现，基于 MyBatis-Plus IService 提供标准 CRUD。
 */
@Service
public class WarehouseServiceImpl extends ServiceImpl<WarehouseMapper, WarehouseEntity> implements WarehouseService {
}
