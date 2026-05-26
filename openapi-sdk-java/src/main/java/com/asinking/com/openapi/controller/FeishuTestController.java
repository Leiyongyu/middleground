package com.asinking.com.openapi.controller;

import com.asinking.com.openapi.common.response.Result;
import com.asinking.com.openapi.config.FeishuProperties;
import com.asinking.com.openapi.service.FeishuAuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 飞书多维表格测试接口，查看表格结构和数据。
 */
@RestController
@RequestMapping("/api/feishu")
public class FeishuTestController {

    private final FeishuProperties properties;
    private final FeishuAuthService authService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FeishuTestController(FeishuProperties properties, FeishuAuthService authService) {
        this.properties = properties;
        this.authService = authService;
    }

    /**
     * 获取表格字段结构（列名、类型）。
     */
    @GetMapping("/fields")
    public Result<Object> fields() throws Exception {
        String token = authService.getAccessToken();
        String url = properties.getEndpoint() + "/open-apis/bitable/v1/apps/"
                + properties.getAppToken() + "/tables/" + properties.getTableId() + "/fields";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
        return Result.ok(objectMapper.readValue(resp.getBody(), Object.class));
    }

    /**
     * 获取表格记录（默认前 10 条）。
     */
    @GetMapping("/records")
    public Result<Object> records(@RequestParam(defaultValue = "10") int pageSize,
                                  @RequestParam(required = false) String pageToken) throws Exception {
        String token = authService.getAccessToken();
        String url = properties.getEndpoint() + "/open-apis/bitable/v1/apps/"
                + properties.getAppToken() + "/tables/" + properties.getTableId() + "/records"
                + "?page_size=" + pageSize;
        if (pageToken != null) {
            url += "&page_token=" + pageToken;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
        return Result.ok(objectMapper.readValue(resp.getBody(), Object.class));
    }
}
