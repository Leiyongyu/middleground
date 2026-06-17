package com.asinking.com.openapi.service;

import com.asinking.com.openapi.config.LingxingProperties;
import com.asinking.com.openapi.entity.AmzWarehouseInventoryDetailEntity;
import com.asinking.com.openapi.mapper.mp.AmzWarehouseInventoryDetailMapper;
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

@Service
public class AmzWarehouseInventoryService extends ServiceImpl<AmzWarehouseInventoryDetailMapper, AmzWarehouseInventoryDetailEntity> {

    private static final Logger LOG = LoggerFactory.getLogger(AmzWarehouseInventoryService.class);
    private static final String API_PATH = "erp/sc/routing/data/local_inventory/inventoryDetails";
    private static final int PAGE_SIZE = 800;

    // Amazon 仓库 wid 列表
    private static final String AMZ_WIDS = "18680";
    //18677,18678,18679,18680,19561

    private final LingxingProperties props;
    private final LingxingAuthService auth;

    public AmzWarehouseInventoryService(LingxingProperties props, LingxingAuthService auth) {
        this.props = props; this.auth = auth;
    }

    public int syncAll() throws Exception {
        // 清空全量重建
        baseMapper.delete(null);
        int total = 0;
        Set<String> seen = new HashSet<>();
        // 逐个WID独立分页，保证数据完整
        String[] wids = AMZ_WIDS.split(",");
        for (String wid : wids) {
            int widTotal = 0;
            int offset = 0;
            for (int guard = 0; guard < 500; guard++) {
                Object resp = callApi(wid, offset, PAGE_SIZE, null);
                ParseResult pr = parseWithTotal(resp);
                if (pr.list.isEmpty()) break;

                List<AmzWarehouseInventoryDetailEntity> fresh = new ArrayList<>();
                for (AmzWarehouseInventoryDetailEntity e : pr.list) {
                    if (seen.add(e.getWid() + "|" + e.getSku())) {
                        fresh.add(e);
                    }
                }
                if (!fresh.isEmpty()) { this.saveBatch(fresh); total += fresh.size(); widTotal += fresh.size(); }
                if (offset + PAGE_SIZE >= pr.total) break;
                offset += PAGE_SIZE;
            }
            LOG.info("[amz-inv] WID={} pulled={}", wid, widTotal);
        }
        LOG.info("[amz-inv] sync done: insert={}", total);
        return total;
    }

    private static class ParseResult {
        List<AmzWarehouseInventoryDetailEntity> list = Collections.emptyList();
        int total = 0;
    }

    private ParseResult parseWithTotal(Object remote) {
        ParseResult r = new ParseResult();
        Map<String, Object> root = JSON.parseObject(JSON.toJSONString(remote), new TypeReference<Map<String, Object>>() {});
        r.total = root.get("total") != null ? ((Number) root.get("total")).intValue() : 0;
        r.list = parseResponse(remote);
        return r;
    }

    @SuppressWarnings("unchecked")
    private List<AmzWarehouseInventoryDetailEntity> parseResponse(Object remote) {
        Map<String, Object> root = JSON.parseObject(JSON.toJSONString(remote), new TypeReference<Map<String, Object>>() {});
        Object dataObj = root.get("data");
        if (!(dataObj instanceof List)) return Collections.emptyList();
        List<Map<String, Object>> dataList = (List<Map<String, Object>>) dataObj;

        List<AmzWarehouseInventoryDetailEntity> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Map<String, Object> row : dataList) {
            Integer wid = getIntObj(row, "wid");
            String sku = getStr(row, "sku");
            if (wid == null || sku.isEmpty()) continue;
            if (!seen.add(wid + "|" + sku)) continue;

            AmzWarehouseInventoryDetailEntity e = new AmzWarehouseInventoryDetailEntity();
            e.setWid(wid);
            e.setSellerId(getStr(row, "seller_id"));
            e.setSku(sku);
            e.setProductValidNum(getInt(row, "product_valid_num"));
            e.setQuantityReceive(getBd(row, "quantity_receive"));
            e.setProductLockNum(getInt(row, "product_lock_num"));
            result.add(e);
        }
        return result;
    }

    public Object debugCallApi(String sku) throws Exception {
        return callApi(AMZ_WIDS, 0, 10, sku);
    }

    private Object callApi(String wid, int offset, int length, String sku) throws Exception {
        String token = auth.getAccessToken();
        Map<String, Object> qp = new LinkedHashMap<>();
        qp.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
        qp.put("access_token", token);
        qp.put("app_key", props.getAppId());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("wid", wid);
        body.put("offset", offset);
        body.put("length", length);
        //body.put("is_sku_merge_show",0);
        if (sku != null && !sku.isEmpty()) body.put("sku", sku);

        Map<String, Object> sm = new LinkedHashMap<>(qp);
        sm.putAll(body);
        qp.put("sign", ApiSign.sign(sm, props.getAppId()));

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

    private String getStr(Map<String, Object> m, String k) { Object v = m.get(k); return v != null ? String.valueOf(v) : ""; }
    private int getInt(Map<String, Object> m, String k) { Object v = m.get(k); if (v instanceof Number) return ((Number)v).intValue(); if (v != null) try { return Integer.parseInt(v.toString()); } catch (Exception e) {} return 0; }
    private BigDecimal getBd(Map<String, Object> m, String k) { Object v = m.get(k); if (v == null) return null; if (v instanceof Number) return BigDecimal.valueOf(((Number)v).doubleValue()); try { return new BigDecimal(v.toString()); } catch (Exception e) { return null; } }
    private Integer getIntObj(Map<String, Object> m, String k) { Object v = m.get(k); if (v instanceof Number) return ((Number)v).intValue(); if (v != null) try { return Integer.parseInt(v.toString()); } catch (Exception e) {} return null; }
}
