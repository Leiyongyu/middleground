package com.asinking.com.openapi.service;

import com.asinking.com.openapi.config.LingxingProperties;
import com.asinking.com.openapi.entity.AmzProductPerformanceEntity;
import com.asinking.com.openapi.mapper.mp.AmzProductPerformanceMapper;
import com.asinking.com.openapi.sdk.core.HttpMethod;
import com.asinking.com.openapi.sdk.core.HttpRequest;
import com.asinking.com.openapi.sdk.core.HttpResponse;
import com.asinking.com.openapi.sdk.okhttp.HttpExecutor;
import com.asinking.com.openapi.sdk.sign.ApiSign;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AmzProductPerformanceService extends ServiceImpl<AmzProductPerformanceMapper, AmzProductPerformanceEntity> {

    private static final Logger LOG = LoggerFactory.getLogger(AmzProductPerformanceService.class);
    private static final String API_PATH = "bd/productPerformance/openApi/asinList";

    private final LingxingProperties props;
    private final LingxingAuthService auth;
    private final LingxingShopService shopService;

    public AmzProductPerformanceService(LingxingProperties props, LingxingAuthService auth,
                                         LingxingShopService shopService) {
        this.props = props; this.auth = auth; this.shopService = shopService;
    }

    /** 每批最多传多少个 SID，避免单次请求数据量过大 */
    private static final int SID_BATCH_SIZE = 20;

    public int syncAll() throws Exception {
        List<String> sids = shopService.getSidsByPlatform(10001);
        if (sids.isEmpty()) { LOG.warn("无Amazon店铺"); return 0; }
        LocalDate end = LocalDate.now().minusDays(1);
        LocalDate start = end.minusDays(91);

        // ① 加载已有记录索引（upsert，不删旧数据以应对 API 不稳定）
        Map<String, Long> existingIds = new LinkedHashMap<>();
        for (AmzProductPerformanceEntity e : baseMapper.selectList(null)) {
            if (e.getSid() != null && e.getSellerSku() != null && !e.getSellerSku().isEmpty()) {
                existingIds.put(e.getSid() + "|" + e.getSellerSku(), e.getId());
            }
        }

        int total = 0;
        List<AmzProductPerformanceEntity> batch = new ArrayList<>();

        // ② 将 SID 分批，每批一次 API 调用
        for (int i = 0; i < sids.size(); i += SID_BATCH_SIZE) {
            List<String> sidBatch = sids.subList(i, Math.min(i + SID_BATCH_SIZE, sids.size()));
            int offset = 0;
            for (int guard = 0; guard < 100; guard++) {
                List<AmzProductPerformanceEntity> list;
                try {
                    Object resp = callApi(sidBatch, start.toString(), end.toString(), offset, 10000);
                    list = parseResponse(resp);
                } catch (Exception ex) {
                    LOG.warn("[amz-perf] API失败 batch-offset={}-{}: {}", i, offset, ex.getMessage());
                    break;
                }
                if (list.isEmpty()) break;

                for (AmzProductPerformanceEntity e : list) {
                    String key = e.getSid() + "|" + e.getSellerSku();
                    Long existId = existingIds.get(key);
                    if (existId != null) e.setId(existId);
                    batch.add(e);
                }
                if (batch.size() >= 5000) {
                    this.saveOrUpdateBatch(batch);
                    batch.forEach(e -> existingIds.put(e.getSid() + "|" + e.getSellerSku(), e.getId()));
                    total += batch.size(); batch.clear();
                }
                if (list.size() < 10000) break;
                offset += 10000;
            }
            if (i + SID_BATCH_SIZE < sids.size()) {
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            }
        }
        if (!batch.isEmpty()) {
            this.saveOrUpdateBatch(batch);
            total += batch.size();
        }

        LOG.info("[amz-perf] sync done: upsert={} (recent 90d)", total);
        return total;
    }

    /** 调试用：调用领星 asinList API 并返回原始响应 */
    public Object debugCallApi(String sid) throws Exception {
        LocalDate end = LocalDate.now().minusDays(1);
        LocalDate start = end.minusDays(91);
        return callApi(Collections.singletonList(sid), start.toString(), end.toString(), 0, 100);
    }

    private Object callApi(List<String> sids, String start, String end, int offset, int length) throws Exception {
        String token = auth.getAccessToken();
        Map<String, Object> qp = new LinkedHashMap<>();
        qp.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
        qp.put("access_token", token); qp.put("app_key", props.getAppId());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("offset", offset); body.put("length", length);
        body.put("sort_field", "volume"); body.put("sort_type", "desc");
        body.put("summary_field", "msku");
        // 单店铺传字符串，多店铺传整数数组（符合领星API规范）
        boolean singleSid = sids.size() == 1;
        if (singleSid) {
            body.put("sid", sids.get(0));  // 字符串 "5608"
        } else {
            // 签名前先放 JSON 字符串，签名后替换回数组（参照 LingxingShopService 的做法）
            body.put("sid", JSON.toJSONString(sids.stream().map(Integer::parseInt).collect(Collectors.toList())));
        }
        body.put("start_date", start); body.put("end_date", end);
        body.put("is_recently_enum", true);

        Map<String, Object> sm = new LinkedHashMap<>(qp); sm.putAll(body);
        qp.put("sign", ApiSign.sign(sm, props.getAppId()));
        // 签名完成后，将 sid 还原为整数数组
        if (!singleSid) {
            body.put("sid", sids.stream().map(Integer::parseInt).collect(Collectors.toList()));
        }

        HttpRequest<Object> req = HttpRequest.builder(Object.class)
                .method(HttpMethod.POST).endpoint(props.getEndpoint()).path(API_PATH)
                .queryParams(qp).json(JSON.toJSONString(body))
                .config(new com.asinking.com.openapi.sdk.core.Config()
                        .withConnectionTimeout(props.getConnectTimeout())
                        .withReadTimeout(props.getReadTimeout()))
                .build();
        HttpResponse hr = HttpExecutor.create().execute(req);
        return hr.readEntity(Object.class);
    }

    private List<AmzProductPerformanceEntity> parseResponse(Object remote) {
        Map<String, Object> root = JSON.parseObject(JSON.toJSONString(remote), new TypeReference<Map<String, Object>>() {});
        Map<String, Object> data = getMap(root, "data");
        if (data == null) return Collections.emptyList();
        List<Map<String, Object>> list = getList(data, "list");
        if (list == null) return Collections.emptyList();

        return list.stream().flatMap(item -> {
            List<Map<String, Object>> prices = getList(item, "price_list");
            if (prices == null) return java.util.stream.Stream.empty();
            return prices.stream().map(pl -> {
                AmzProductPerformanceEntity e = new AmzProductPerformanceEntity();
                // price_list 层
                e.setSid(getInt(pl, "sid"));
                e.setSellerSku(getStr(pl, "seller_sku"));
                // item 层（只需要评分和评论数）
                e.setAvgStar(bdOrNull(item, "avg_star"));
                e.setReviewsCount(getInt(item, "reviews_count"));
                return e;
            });
        }).collect(Collectors.toList());
    }

    private String getStr(Map<String, Object> m, String k) { Object v = m.get(k); return v != null ? String.valueOf(v) : ""; }
    private Integer getInt(Map<String, Object> m, String k) { Object v = m.get(k); if (v instanceof Number) return ((Number)v).intValue(); if (v != null) try { return Integer.parseInt(v.toString()); } catch (Exception e) {} return 0; }
    private BigDecimal bdOrNull(Map<String, Object> m, String k) { Object v = m.get(k); if (v == null) return null; if (v instanceof Number) return BigDecimal.valueOf(((Number)v).doubleValue()); try { return new BigDecimal(v.toString()); } catch (Exception e) { return null; } }
    @SuppressWarnings("unchecked")
    private Map<String, Object> getMap(Map<String, Object> m, String k) { Object v = m.get(k); return v instanceof Map ? (Map<String, Object>) v : null; }
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getList(Object obj, String k) { if (!(obj instanceof Map)) return null; Object v = ((Map<?,?>)obj).get(k); return v instanceof List ? (List<Map<String, Object>>) v : null; }
}
