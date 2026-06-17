package com.asinking.com.openapi.service;

import com.asinking.com.openapi.config.LingxingProperties;
import com.asinking.com.openapi.entity.AmzProductListingEntity;
import com.asinking.com.openapi.mapper.mp.AmzProductListingMapper;
import com.asinking.com.openapi.sdk.core.HttpMethod;
import com.asinking.com.openapi.sdk.core.HttpRequest;
import com.asinking.com.openapi.sdk.core.HttpResponse;
import com.asinking.com.openapi.sdk.okhttp.HttpExecutor;
import com.asinking.com.openapi.sdk.sign.ApiSign;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 领星 Amazon Listing 同步服务。
 */
@Service
public class LingxingAmazonService {

    private static final String LISTING_PATH = "erp/sc/data/mws/listing";
    private static final int PAGE_SIZE = 200;

    private final LingxingProperties properties;
    private final LingxingAuthService authService;
    private final AmzProductListingMapper mapper;

    public LingxingAmazonService(LingxingProperties properties,
                                  LingxingAuthService authService,
                                  AmzProductListingMapper mapper) {
        this.properties = properties;
        this.authService = authService;
        this.mapper = mapper;
    }

    /** 全量同步 Amazon Listing */
    /** 测试：拉取指定 sid 的 listing 原始响应，支持搜索条件 */
    public Object testFetchListing(List<String> sids, String searchField, String searchValue) throws Exception {
        String accessToken = authService.getAccessToken();
        Map<String, Object> qp = new LinkedHashMap<>();
        qp.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
        qp.put("access_token", accessToken);
        qp.put("app_key", properties.getAppId());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sid", String.join(",", sids));
        body.put("is_pair", 1);
        body.put("is_delete", 0);
        body.put("offset", 0);
        body.put("length", 5);
        if (searchField != null && !searchField.isEmpty()) body.put("search_field", searchField);
        if (searchValue != null && !searchValue.isEmpty()) {
            body.put("search_value", JSON.toJSONString(Collections.singletonList(searchValue)));
        }

        Map<String, Object> sm = new LinkedHashMap<>(qp);
        sm.putAll(body);
        qp.put("sign", ApiSign.sign(sm, properties.getAppId()));
        if (searchValue != null && !searchValue.isEmpty()) {
            body.put("search_value", Collections.singletonList(searchValue));
        }

        return HttpExecutor.create().execute(HttpRequest.builder(Object.class)
                .method(HttpMethod.POST).endpoint(properties.getEndpoint()).path(LISTING_PATH)
                .queryParams(qp).json(JSON.toJSONString(body))
                .config(new com.asinking.com.openapi.sdk.core.Config()
                        .withConnectionTimeout(properties.getConnectTimeout())
                        .withReadTimeout(properties.getReadTimeout()))
                .build()).readEntity(Object.class);
    }

    public int syncAllAmzListings(List<String> sids) throws Exception {
        if (sids == null || sids.isEmpty()) return 0;
        // 清空全量重建
        mapper.delete(null);
        int total = 0;
        // 每批 20 个 SID，避免一次传太多导致 API 只处理部分
        int batchSize = 20;
        for (int i = 0; i < sids.size(); i += batchSize) {
            List<String> batch = sids.subList(i, Math.min(i + batchSize, sids.size()));
            int offset = 0;
            for (int guard = 0; guard < 200; guard++) {
                Object remote = requestAmzListing(batch, offset, PAGE_SIZE);
                List<AmzProductListingEntity> list = parseListings(remote);
                if (list.isEmpty()) break;
                for (AmzProductListingEntity e : list) mapper.insert(e);
                total += list.size();
                if (list.size() < PAGE_SIZE) break;
                offset += PAGE_SIZE;
            }
            if (i + batchSize < sids.size()) {
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            }
        }
        return total;
    }

    @SuppressWarnings("unchecked")
    private List<AmzProductListingEntity> parseListings(Object remote) {
        Map<String, Object> root = JSON.parseObject(JSON.toJSONString(remote), new TypeReference<Map<String, Object>>() {});
        List<Map<String, Object>> data = getList(root, "data");
        if (data.isEmpty()) return Collections.emptyList();
        List<AmzProductListingEntity> result = new ArrayList<>();
        for (Map<String, Object> row : data) {
            Integer sid = getIntObj(row, "sid");
            String sellerSku = getStr(row, "seller_sku");
            if (sellerSku.isEmpty() || sid == null) continue;
            AmzProductListingEntity e = new AmzProductListingEntity();
            e.setSid(sid);
            e.setSellerSku(sellerSku);
            fillFields(e, row);
            result.add(e);
        }
        return result;
    }

    private Object requestAmzListing(List<String> sids, int offset, int length) throws Exception {
        String accessToken = authService.getAccessToken();
        Map<String, Object> qp = new LinkedHashMap<>();
        qp.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
        qp.put("access_token", accessToken);
        qp.put("app_key", properties.getAppId());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sid", String.join(",", sids));
        body.put("is_pair", 1);
        body.put("is_delete", 0);
        body.put("offset", offset);
        body.put("length", length);

        Map<String, Object> signMap = new LinkedHashMap<>(qp);
        signMap.putAll(body);
        qp.put("sign", ApiSign.sign(signMap, properties.getAppId()));

        HttpRequest<Object> request = HttpRequest.builder(Object.class)
                .method(HttpMethod.POST)
                .endpoint(properties.getEndpoint())
                .path(LISTING_PATH)
                .queryParams(qp)
                .json(JSON.toJSONString(body))
                .config(new com.asinking.com.openapi.sdk.core.Config()
                        .withConnectionTimeout(properties.getConnectTimeout())
                        .withReadTimeout(properties.getReadTimeout()))
                .build();

        HttpResponse response = HttpExecutor.create().execute(request);
        return response.readEntity(Object.class);
    }

    private void fillFields(AmzProductListingEntity e, Map<String, Object> row) {
        e.setMarketplace(getStr(row, "marketplace"));
        e.setAsin(getStr(row, "asin"));
        e.setLocalSku(getStr(row, "local_sku"));
        e.setLocalName(getStr(row, "local_name"));
        e.setReviewNum(getInt(row, "review_num"));
        e.setLastStar(getStr(row, "last_star"));
        // principal_info 是数组: [{ principal_name: "xxx", ... }]
        List<Map<String, Object>> piList = getList(row, "principal_info");
        if (piList != null && !piList.isEmpty()) {
            e.setPrincipalName(getStr(piList.get(0), "principal_name"));
        }
    }

    // ==== helpers ====
    private String getStr(Map<String, Object> m, String k) { Object v = m.get(k); return v != null ? String.valueOf(v) : ""; }
    private BigDecimal getBd(Map<String, Object> m, String k) {
        Object v = m.get(k); if (v == null) return BigDecimal.ZERO;
        if (v instanceof Number) return BigDecimal.valueOf(((Number)v).doubleValue());
        try { return new BigDecimal(String.valueOf(v)); } catch (Exception e) { return BigDecimal.ZERO; }
    }
    private Integer getInt(Map<String, Object> m, String k) {
        Object v = m.get(k); if (v == null) return 0;
        if (v instanceof Number) return ((Number)v).intValue();
        try { return Integer.parseInt(String.valueOf(v)); } catch (Exception e) { return 0; }
    }
    private Integer getIntObj(Map<String, Object> m, String k) {
        Object v = m.get(k); if (v == null) return null;
        if (v instanceof Number) return ((Number)v).intValue();
        try { return Integer.parseInt(String.valueOf(v)); } catch (Exception e) { return null; }
    }
    @SuppressWarnings("unchecked")
    private Map<String, Object> getMap(Map<String, Object> m, String k) { Object v = m.get(k); return v instanceof Map ? (Map<String, Object>) v : null; }
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getList(Map<String, Object> m, String k) {
        Object v = m.get(k); return v instanceof List ? (List<Map<String, Object>>) v : Collections.emptyList();
    }
}
