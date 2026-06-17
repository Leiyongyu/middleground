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

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AmzRestockSummaryService extends ServiceImpl<AmzRestockSummaryMapper, AmzRestockSummaryEntity> {

    private static final Logger LOG = LoggerFactory.getLogger(AmzRestockSummaryService.class);
    private static final String API_PATH = "erp/sc/routing/restocking/analysis/getSummaryList";
    private static final int PAGE_SIZE = 50;
    private static final int SID_BATCH_SIZE = 20;

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

        // 清空全量重建
        baseMapper.delete(null);

        int total = 0;
        Set<String> seen = new HashSet<>();

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

                List<AmzRestockSummaryEntity> fresh = new ArrayList<>();
                for (AmzRestockSummaryEntity e : list) {
                    if (seen.add(e.getHashId())) fresh.add(e);
                }
                if (!fresh.isEmpty()) { this.saveBatch(fresh); total += fresh.size(); }
                if (list.size() < PAGE_SIZE) break;
                offset += PAGE_SIZE;
            }
            if (i + SID_BATCH_SIZE < sids.size()) {
                try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
            }
        }
        LOG.info("[amz-restock] sync done: upsert={}", total);
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
            addIfValid(result, item);
            // 遍历子项 item_list
            List<Map<String, Object>> children = getList(item, "item_list");
            if (children != null) {
                for (Map<String, Object> child : children) {
                    addIfValid(result, child);
                }
            }
        }
        return result;
    }

    private void addIfValid(List<AmzRestockSummaryEntity> result, Map<String, Object> item) {
        Map<String, Object> basic = getMap(item, "basic_info");
        if (basic == null) return;

        String hashId = getStr(basic, "hash_id");
        if (hashId.isEmpty()) return;

        Integer sid = getIntObj(basic, "sid");
        String msku = firstMsku(basic);
        if (sid == null || msku == null || msku.isEmpty()) return;

        Map<String, Object> qty = getMap(item, "amazon_quantity_info");
        Map<String, Object> sales = getMap(item, "sales_info");

        AmzRestockSummaryEntity e = new AmzRestockSummaryEntity();
        e.setHashId(hashId);
        e.setNodeType(getIntObj(basic, "node_type"));
        e.setSid(sid);
        e.setMsku(msku);
        e.setSyncTime(getStr(basic, "sync_time"));
        e.setFbaSellable(qty != null ? getInt(qty, "amazon_quantity_valid") : 0);
        e.setFbaInbound(qty != null ? getInt(qty, "amazon_quantity_shipping") : 0);
        e.setFbaReserved(qty != null ? getInt(qty, "afn_reserved_quantity") : 0);
        e.setSales7d(sales != null ? getInt(sales, "sales_total_7") : 0);
        e.setSales14d(sales != null ? getInt(sales, "sales_total_14") : 0);
        e.setSales30d(sales != null ? getInt(sales, "sales_total_30") : 0);
        e.setSales60d(sales != null ? getInt(sales, "sales_total_60") : 0);
        e.setAvgSales14d(sales != null ? getBd(sales, "sales_avg_14") : null);
        e.setAvgSales30d(sales != null ? getBd(sales, "sales_avg_30") : null);
        e.setAvgSales60d(sales != null ? getBd(sales, "sales_avg_60") : null);
        result.add(e);
    }

    @SuppressWarnings("unchecked")
    private String firstMsku(Map<String, Object> basic) {
        Object listObj = basic.get("msku_fnsku_list");
        if (listObj instanceof List) {
            List<Map<String, Object>> list = (List<Map<String, Object>>) listObj;
            if (!list.isEmpty()) {
                Object msku = list.get(0).get("msku");
                return msku != null ? String.valueOf(msku) : "";
            }
        }
        return "";
    }

    private Object callApi(List<String> sids, int offset, int length) throws Exception {
        String token = auth.getAccessToken();
        Map<String, Object> qp = new LinkedHashMap<>();
        qp.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
        qp.put("access_token", token); qp.put("app_key", props.getAppId());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("data_type", 2);
        body.put("offset", offset);
        body.put("length", length);
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
    private String getStr(Map<String, Object> m, String k) { Object v = m.get(k); return v != null ? String.valueOf(v) : ""; }
    private int getInt(Map<String, Object> m, String k) { Object v = m.get(k); if (v instanceof Number) return ((Number)v).intValue(); if (v != null) try { return Integer.parseInt(v.toString()); } catch (Exception e) {} return 0; }
    private BigDecimal getBd(Map<String, Object> m, String k) { Object v = m.get(k); if (v == null) return null; if (v instanceof Number) return BigDecimal.valueOf(((Number)v).doubleValue()); try { return new BigDecimal(v.toString()); } catch (Exception e) { return null; } }
    private Integer getIntObj(Map<String, Object> m, String k) { Object v = m.get(k); if (v instanceof Number) return ((Number)v).intValue(); if (v != null) try { return Integer.parseInt(v.toString()); } catch (Exception e) {} return null; }
    @SuppressWarnings("unchecked")
    private Map<String, Object> getMap(Map<String, Object> m, String k) { Object v = m.get(k); return v instanceof Map ? (Map<String, Object>) v : null; }
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getList(Map<String, Object> m, String k) { Object v = m.get(k); return v instanceof List ? (List<Map<String, Object>>) v : null; }
}
