package com.asinking.com.openapi.service.impl;

import com.asinking.com.openapi.entity.EbayProductListingEntity;
import com.asinking.com.openapi.mapper.mp.EbayProductListingMapper;
import com.asinking.com.openapi.service.EbayProductListingService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * eBay 商品 Listing 业务实现，基于 MyBatis-Plus IService 提供标准 CRUD。
 */
@Service
public class EbayProductListingServiceImpl extends ServiceImpl<EbayProductListingMapper, EbayProductListingEntity> implements EbayProductListingService {
}
