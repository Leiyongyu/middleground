package com.asinking.com.openapi.service;

import com.asinking.com.openapi.config.LingxingProperties;
import com.asinking.com.openapi.entity.AmzRestockSummaryEntity;
import com.asinking.com.openapi.mapper.mp.AmzRestockSummaryMapper;
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

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AmzRestockSummaryService extends ServiceImpl<AmzRestockSummaryMapper, AmzRestockSummaryEntity> {

    private static final Logger LOG = LoggerFactory.getLogger(AmzRestockSummaryService.class);
    private static final String API_PATH = "erp/sc/routing/restocking/analysis/getSummaryList";
    private static final int PAGE_SIZE = 50;          // 接口上限50
    private static final int SID_BATCH_SIZE = 20;     // 每批传20个SID

    private final LingxingProperties props;
    private final LingxingAuthService auth;
    private final LingxingShopService shopService;

    public AmzRestockSummaryService(LingxingProperties props, LingxingAuthService auth,
                                     LingxingShopService shopService) {
        this.props = props; this.auth = auth; this.shopService = shopService;
    }

    public int syncAll() throws Exception {
        List<String> sids = shopService.getSidsByPlatform(10001);
        if (sids.isEmpty()) { LOG.warn("[amz-restock] 无Amazon店铺"); return 0; }

        // 清空表全量重建
        baseMapper.delete(null);
        int total = 0;
        List<AmzRestockSummaryEntity> batch = new ArrayList<>();

        for (int i = 0; i < sids.size(); i += SID_BATCH_SIZE) {
            List<String> sidBatch = sids.subList(i, Math.min(i + SID_BATCH_SIZE, sids.size()));
            int offset = 0;
            for (int guard = 0; guard < 200; guard++) {
                List<AmzRestockSummaryEntity> list;
                try {
                    Object resp = callApi(sidBatch, offset, PAGE_SIZE);
                    list = parseResponse(resp);
                } catch (Exception ex) {
                    LOG.warn("[amz-restock] API失败 batch-offset={}-{}: {}", i, offset, ex.getMessage());
                    break;
                }
                if (list.isEmpty()) break;

                batch.addAll(list);
                if (batch.size() >= 1000) {
                    this.saveBatch(batch);
                    total += batch.size(); batch.clear();
                }
                if (list.size() < PAGE_SIZE) break;
                offset += PAGE_SIZE;
            }
            // 令牌桶=1，批次间休息2s
            if (i + SID_BATCH_SIZE < sids.size()) {
                try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
            }
        }
        if (!batch.isEmpty()) {
            this.saveBatch(batch);
            total += batch.size();
        }

        LOG.info("[amz-restock] sync done: insert={}", total);
        return total;
    }

    @SuppressWarnings("unchecked")
    private List<AmzRestockSummaryEntity> parseResponse(Object remote) {
        Map<String, Object> root = JSON.parseObject(JSON.toJSONString(remote), new TypeReference<Map<String, Object>>() {});
        Object dataObj = root.get("data");
        if (!(dataObj instanceof List)) return Collections.emptyList();
        List<Map<String, Object>> dataList = (List<Map<String, Object>>) dataObj;

        List<AmzRestockSummaryEntity> result = new ArrayList<>();
        for (Map<String, Object> item : dataList) {
            Map<String, Object> basic = getMap(item, "basic_info");
            if (basic == null) continue;

            // 只取 node_type=3(非共享库存) 或 node_type=4(汇总行)，filter掉父子行避免重复
            Integer nodeType = getIntObj(basic, "node_type");
            if (nodeType == null || nodeType == 1 || nodeType == 2) continue;

            Integer sid = getIntObj(basic, "sid");
            String msku = firstMsku(basic);
            if (sid == null || msku == null || msku.isEmpty()) continue;

            Map<String, Object> qty = getMap(item, "amazon_quantity_info");
            int fbaSellable = qty != null ? getInt(qty, "amazon_quantity_valid") : 0;
            int fbaInbound = qty != null ? getInt(qty, "amazon_quantity_shipping") : 0;

            AmzRestockSummaryEntity e = new AmzRestockSummaryEntity();
            e.setSid(sid);
            e.setMsku(msku);
            e.setFbaSellable(fbaSellable);
            e.setFbaInbound(fbaInbound);
            result.add(e);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private String firstMsku(Map<String, Object> basic) {
        Object listObj = basic.get("msku_fnsku_list");
        if (listObj instanceof List) {
            List<Map<String, Object>> list = (List<Map<String, Object>>) listObj;
            if (!list.isEmpty()) {
                Object msku = list.get(0).get("msku");
                return msku != null ? String.valueOf(msku) : null;
            }
        }
        return null;
    }

    private Object callApi(List<String> sids, int offset, int length) throws Exception {
        String token = auth.getAccessToken();
        Map<String, Object> qp = new LinkedHashMap<>();
        qp.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
        qp.put("access_token", token); qp.put("app_key", props.getAppId());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("data_type", 2);  // msku维度
        body.put("offset", offset);
        body.put("length", length);
        // sid_list: 签名前JSON串，签名后还原数组
        body.put("sid_list", JSON.toJSONString(sids));

        Map<String, Object> sm = new LinkedHashMap<>(qp); sm.putAll(body);
        qp.put("sign", ApiSign.sign(sm, props.getAppId()));
        body.put("sid_list", sids);

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
    private int getInt(Map<String, Object> m, String k) { Object v = m.get(k); if (v instanceof Number) return ((Number)v).intValue(); if (v != null) try { return Integer.parseInt(v.toString()); } catch (Exception e) {} return 0; }
    private Integer getIntObj(Map<String, Object> m, String k) { Object v = m.get(k); if (v instanceof Number) return ((Number)v).intValue(); if (v != null) try { return Integer.parseInt(v.toString()); } catch (Exception e) {} return null; }
    @SuppressWarnings("unchecked")
    private Map<String, Object> getMap(Map<String, Object> m, String k) { Object v = m.get(k); return v instanceof Map ? (Map<String, Object>) v : null; }
}
