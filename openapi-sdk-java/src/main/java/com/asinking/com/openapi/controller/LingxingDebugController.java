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

    /** 调试：通用 POST 到领星 API，body 可传 _endpoint 覆盖网关地址 */
    @PostMapping("/raw")
    public Object rawPost(@RequestBody Map<String, Object> body,
                          @RequestParam(defaultValue = "") String path) throws Exception {
        String token = authService.getAccessToken();
        String endpoint = body.containsKey("_endpoint")
            ? String.valueOf(body.remove("_endpoint"))
            : "http://8.137.177.25/lingxing-proxy";

        Map<String, Object> qp = new LinkedHashMap<>();
        qp.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
        qp.put("access_token", token);
        qp.put("app_key", "ak_DBmvLSyHfhc5H");

        Map<String, Object> sm = new LinkedHashMap<>(qp);
        sm.putAll(body);
        qp.put("sign", com.asinking.com.openapi.sdk.sign.ApiSign.sign(sm, "8VSwwqXgu/RtUvslYIacHQ=="));

        com.asinking.com.openapi.sdk.core.HttpRequest<Object> req =
            com.asinking.com.openapi.sdk.core.HttpRequest.builder(Object.class)
                .method(com.asinking.com.openapi.sdk.core.HttpMethod.POST)
                .endpoint(endpoint).path(path)
                .queryParams(qp)
                .json(com.alibaba.fastjson2.JSON.toJSONString(body))
                .config(new com.asinking.com.openapi.sdk.core.Config()
                        .withConnectionTimeout(60000).withReadTimeout(120000))
                .build();
        return com.asinking.com.openapi.sdk.okhttp.HttpExecutor.create().execute(req).readEntity(Object.class);
    }

}
