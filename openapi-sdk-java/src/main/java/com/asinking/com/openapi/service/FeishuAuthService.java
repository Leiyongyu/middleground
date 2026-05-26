package com.asinking.com.openapi.service;

import com.asinking.com.openapi.config.FeishuProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 飞书认证服务：获取 tenant_access_token，带内存缓存和提前刷新。
 */
@Service
public class FeishuAuthService {

    private static final Logger LOG = LoggerFactory.getLogger(FeishuAuthService.class);

    private final FeishuProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private volatile String token;
    private volatile long expiresAt;

    public FeishuAuthService(FeishuProperties properties) {
        this.properties = properties;
    }

    public synchronized String getAccessToken() throws Exception {
        if (token != null && System.currentTimeMillis() < expiresAt - 300_000) {
            return token;
        }
        String url = properties.getEndpoint() + "/open-apis/auth/v3/tenant_access_token/internal";

        Map<String, String> body = new java.util.HashMap<>();
        body.put("app_id", properties.getAppId());
        body.put("app_secret", properties.getAppSecret());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> resp = restTemplate.postForEntity(url, request, String.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> result = objectMapper.readValue(resp.getBody(), Map.class);
        Integer code = (Integer) result.get("code");
        if (code != null && code != 0) {
            throw new RuntimeException("飞书获取token失败: " + result.get("msg"));
        }

        token = (String) result.get("tenant_access_token");
        Integer expire = (Integer) result.get("expire");
        expiresAt = System.currentTimeMillis() + (expire != null ? expire : 7200) * 1000L;
        LOG.info("飞书 token 已刷新, 过期时间: {}", expiresAt);
        return token;
    }
}
