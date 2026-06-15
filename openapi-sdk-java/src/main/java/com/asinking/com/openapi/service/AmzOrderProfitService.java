package com.asinking.com.openapi.service;

import com.asinking.com.openapi.config.LingxingProperties;
import com.asinking.com.openapi.entity.AmzOrderProfitEntity;
import com.asinking.com.openapi.mapper.mp.AmzOrderProfitMapper;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AmzOrderProfitService extends ServiceImpl<AmzOrderProfitMapper, AmzOrderProfitEntity> {

    private static final Logger LOG = LoggerFactory.getLogger(AmzOrderProfitService.class);
    private static final String API_PATH = "basicOpen/finance/mreport/OrderProfit";
    private static final int PAGE_SIZE = 5000;
    private static final int SID_BATCH_SIZE = 20;

    private final LingxingProperties props;
    private final LingxingAuthService auth;
    private final LingxingShopService shopService;

    public AmzOrderProfitService(LingxingProperties props, LingxingAuthService auth,
                                  LingxingShopService shopService) {
        this.props = props; this.auth = auth; this.shopService = shopService;
    }

    public int syncAll() throws Exception {
        List<String> sids = shopService.getSidsByPlatform(10001);
        if (sids.isEmpty()) { LOG.warn("[amz-profit] 无Amazon店铺"); return 0; }
        LocalDate end = LocalDate.now().minusDays(1);
        LocalDate start = end.minusDays(30);

        // ① 清空表全量重建
        baseMapper.delete(null);
        int total = 0;
        List<AmzOrderProfitEntity> batch = new ArrayList<>();

        // ② 分批调用 API
        for (int i = 0; i < sids.size(); i += SID_BATCH_SIZE) {
            List<String> sidBatch = sids.subList(i, Math.min(i + SID_BATCH_SIZE, sids.size()));
            int offset = 0;
            for (int guard = 0; guard < 200; guard++) {
                List<AmzOrderProfitEntity> list;
                try {
                    Object resp = callApi(sidBatch, start.toString(), end.toString(), offset, PAGE_SIZE);
                    list = parseResponse(resp);
                } catch (Exception ex) {
                    LOG.warn("[amz-profit] API失败 batch-offset={}-{}: {}", i, offset, ex.getMessage());
                    break;
                }
                if (list.isEmpty()) break;

                batch.addAll(list);
                if (batch.size() >= 5000) {
                    this.saveBatch(batch);
                    total += batch.size(); batch.clear();
                }
                if (list.size() < PAGE_SIZE) break;
                offset += PAGE_SIZE;
            }
            if (i + SID_BATCH_SIZE < sids.size()) {
                try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
            }
        }
        if (!batch.isEmpty()) {
            this.saveBatch(batch);
            total += batch.size();
        }
        LOG.info("[amz-profit] sync done: insert={} (recent 30d)", total);
        return total;
    }

    @SuppressWarnings("unchecked")
    private List<AmzOrderProfitEntity> parseResponse(Object remote) {
        Map<String, Object> root = JSON.parseObject(JSON.toJSONString(remote), new TypeReference<Map<String, Object>>() {});
        Object dataObj = root.get("data");
        if (!(dataObj instanceof List)) return Collections.emptyList();
        List<Map<String, Object>> dataList = (List<Map<String, Object>>) dataObj;

        List<AmzOrderProfitEntity> result = new ArrayList<>();
        for (Map<String, Object> item : dataList) {
            // 从 price_list 取 sid + seller_sku
            List<Map<String, Object>> priceList = getList(item, "price_list");
            if (priceList == null || priceList.isEmpty()) continue;
            Map<String, Object> pl = priceList.get(0);

            AmzOrderProfitEntity e = new AmzOrderProfitEntity();
            e.setSid(getInt(pl, "sid"));
            e.setSellerSku(getStr(pl, "seller_sku"));
            e.setGrossMargin(getBd(item, "gross_margin"));
            e.setSpendRate(getBd(item, "spend_rate"));
            e.setRefundAmountRate(getBd(item, "refund_amount_rate"));
            result.add(e);
        }
        return result;
    }

    private Object callApi(List<String> sids, String start, String end, int offset, int length) throws Exception {
        String token = auth.getAccessToken();
        Map<String, Object> qp = new LinkedHashMap<>();
        qp.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
        qp.put("access_token", token);
        qp.put("app_key", props.getAppId());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("offset", offset);
        body.put("length", length);
        body.put("startDate", start);
        body.put("endDate", end);
        // sids: 签名前 JSON 字符串，签名后还原数组
        body.put("sids", JSON.toJSONString(sids.stream().map(Integer::parseInt).collect(Collectors.toList())));

        Map<String, Object> sm = new LinkedHashMap<>(qp);
        sm.putAll(body);
        qp.put("sign", ApiSign.sign(sm, props.getAppId()));
        body.put("sids", sids.stream().map(Integer::parseInt).collect(Collectors.toList()));

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

    // ---- helpers ----
    private String getStr(Map<String, Object> m, String k) { Object v = m.get(k); return v != null ? String.valueOf(v) : ""; }
    private Integer getInt(Map<String, Object> m, String k) { Object v = m.get(k); if (v instanceof Number) return ((Number)v).intValue(); if (v != null) try { return Integer.parseInt(v.toString()); } catch (Exception e) {} return 0; }
    private BigDecimal getBd(Map<String, Object> m, String k) { Object v = m.get(k); if (v == null) return BigDecimal.ZERO; if (v instanceof Number) return BigDecimal.valueOf(((Number)v).doubleValue()); try { return new BigDecimal(v.toString()); } catch (Exception e) { return BigDecimal.ZERO; } }
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getList(Object obj, String k) { if (!(obj instanceof Map)) return null; Object v = ((Map<?,?>)obj).get(k); return v instanceof List ? (List<Map<String, Object>>) v : null; }
}
