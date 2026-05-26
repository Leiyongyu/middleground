package com.asinking.com.openapi.service.impl;

import com.asinking.com.openapi.entity.EbayShopListEntity;
import com.asinking.com.openapi.mapper.mp.EbayShopListMapper;
import com.asinking.com.openapi.service.EbayShopListService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * eBay 店铺列表业务实现，基于 MyBatis-Plus IService 提供标准 CRUD。
 */
@Service
public class EbayShopListServiceImpl extends ServiceImpl<EbayShopListMapper, EbayShopListEntity> implements EbayShopListService {
}
