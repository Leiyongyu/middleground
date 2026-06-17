package com.asinking.com.openapi.controller;

import com.asinking.com.openapi.service.LingxingAuthService;
import com.asinking.com.openapi.service.AmazonComputeService;
import com.asinking.com.openapi.service.LingxingAmazonService;
import com.asinking.com.openapi.service.LingxingEbayService;
import com.asinking.com.openapi.service.LingxingPlatformOrderService;
import com.asinking.com.openapi.service.LingxingShopService;
import com.asinking.com.openapi.service.model.EbayListRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 */
@RestController
@RequestMapping("/api/lingxing")
public class LingxingDebugController {

    private final LingxingAuthService authService;
    private final LingxingShopService shopService;
    private final LingxingEbayService ebayService;
    private final LingxingPlatformOrderService platformOrderService;
    private final LingxingAmazonService amazonService;
    private final AmazonComputeService amazonComputeService;

    public LingxingDebugController(LingxingAuthService authService, LingxingShopService shopService,
                                   LingxingEbayService ebayService, LingxingPlatformOrderService platformOrderService,
                                   LingxingAmazonService amazonService, AmazonComputeService amazonComputeService) {
        this.authService = authService;
        this.shopService = shopService;
        this.ebayService = ebayService;
        this.platformOrderService = platformOrderService;
        this.amazonService = amazonService;
        this.amazonComputeService = amazonComputeService;
    }

    /**
     * 获取领星 access_token（调试用）。
     */
    @GetMapping("/token")
    public Object getToken() throws Exception {
        return authService.getAccessTokenResponse();
    }

    /**
     * 查询启用的 eBay 店铺列表。
     */
    @GetMapping("/shops/ebay")
    public Object getActiveEbayShops(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "200") int length) throws Exception {
        return shopService.getActiveShops(10003, offset, length);
    }

    @GetMapping("/shops/amz")
    public Object getActiveAmzShops(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "200") int length) throws Exception {
        return shopService.getActiveShops(10001, offset, length);
    }

    @PostMapping("/amz/refresh-snapshot")
    public Object refreshAmzSnapshot() {
        amazonComputeService.refreshSnapshot();
        return "ok";
    }

    @GetMapping("/amz/listing")
    public Object syncAmzListing(@RequestParam(defaultValue = "0") int offset,
                                  @RequestParam(defaultValue = "200") int length) throws Exception {
        List<String> sids = shopService.getSidsByPlatform(10001);
        if (sids.isEmpty()) return "No Amazon shops found";
        return amazonService.syncAllAmzListings(sids);
    }

    /**
     * 分页查询 eBay 商品 listing。
     */
    @PostMapping("/ebay/list")
    public Object listEbayItems(@RequestBody(required = false) EbayListRequest req) throws Exception {
        return ebayService.listEbayItems(req);
    }

    /**
     * 全量同步 eBay 商品 listing 到本地库。
     */
    @PostMapping("/ebay/list/all")
    public Object syncAllEbayItems(@RequestBody(required = false) EbayListRequest req) throws Exception {
        return ebayService.syncAllEbayItems(req);
    }

    /** 测试：获取平台订单并查发货详情 */
    @GetMapping("/order/test")
    public Object testOrder(@RequestParam(defaultValue = "2026-05-01 00:00:00") String startDate,
                            @RequestParam(defaultValue = "2026-05-25 23:59:59") String endDate) throws Exception {
        return platformOrderService.testFetchOne(startDate, endDate);
    }

    /** 调试：测试 listing API 返回原始数据 */
    @GetMapping("/amz-listing-test")
    public Object testAmzListing(@RequestParam(defaultValue = "") String searchField,
                                  @RequestParam(defaultValue = "") String searchValue) throws Exception {
        List<String> sids = shopService.getSidsByPlatform(10001);
        return amazonService.testFetchListing(sids, searchField, searchValue);
    }

}
