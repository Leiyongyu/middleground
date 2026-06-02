package com.asinking.com.openapi.config;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * JWT Token 黑名单：基于 Redis 存储，支持多实例共享，重启不丢失。
 * Key: jwt:blacklist:{jti}，TTL 对齐 Token 剩余有效期，过期自动清除。
 */
@Component
public class TokenBlacklist {

    private static final String KEY_PREFIX = "jwt:blacklist:";
    private final StringRedisTemplate redis;

    public TokenBlacklist(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** 将指定 JTI 加入黑名单，有效期 = Token 剩余存活时间 */
    public void revoke(String jti, long expMillis) {
        if (jti == null || jti.isEmpty()) return;
        long ttlMillis = expMillis - System.currentTimeMillis();
        if (ttlMillis <= 0) return;
        redis.opsForValue().set(KEY_PREFIX + jti, "1", ttlMillis, TimeUnit.MILLISECONDS);
    }

    /** 判断 JTI 是否已被撤销 */
    public boolean isRevoked(String jti) {
        if (jti == null || jti.isEmpty()) return false;
        return Boolean.TRUE.equals(redis.hasKey(KEY_PREFIX + jti));
    }
}
