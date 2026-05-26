package com.asinking.com.openapi.sdk.sign;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

public class ApiSign {

    private static final Logger logger = LoggerFactory.getLogger(ApiSign.class);

    /**
     * 领星 OpenAPI 签名：参数按 key 排序拼成 k1=v1&k2=v2，MD5 后 AES-ECB 加密。
     * 列表中文字段在签名时传 JSON 字符串形式，签名完成后再替换回数组。
     */
    public static String sign(Map<String, Object> params, String appSecret) {
        String[] keys = params.keySet().toArray(new String[0]);
        Arrays.sort(keys);
        StringBuilder paramNameValue = new StringBuilder();
        for (String key : keys) {
            String value = String.valueOf(params.get(key));
            paramNameValue.append(key).append("=").append(value.trim()).append("&");
        }
        String paramValue = paramNameValue.toString();
        if (paramValue.endsWith("&")) {
            paramValue = paramValue.substring(0, paramValue.length() - 1);
        }
        String md5Hex = DigestUtils.md5Hex(paramValue.getBytes(StandardCharsets.UTF_8)).toUpperCase();
        return AesUtil.encryptEcb(md5Hex, appSecret);
    }
}
