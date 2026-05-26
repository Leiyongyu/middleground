package com.asinking.com.openapi.utils;

import com.asinking.com.openapi.config.JwtProperties;
import com.asinking.com.openapi.entity.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.UUID;

/**
 * JWT Token 服务：签发和解析 JWT token，含 issuer/subject/uid/role/jti 等 claims。
 */
@Service
public class JwtTokenService {

    private final JwtProperties jwtProperties;
    private final Key signingKey;

    public JwtTokenService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.signingKey = Keys.hmacShaKeyFor(resolveSecret(jwtProperties.getSecret()));
    }

    public TokenInfo issue(UserEntity user) {
        long now = System.currentTimeMillis();
        long expMillis = now + jwtProperties.getTtlSeconds() * 1000L;

        String jti = UUID.randomUUID().toString().replace("-", "");
        String token = Jwts.builder()
                .setIssuer(jwtProperties.getIssuer())
                .setSubject(user.getAccount())
                .setId(jti)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(expMillis))
                .claim("uid", user.getId())
                .claim("role", user.getRole())
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();

        return new TokenInfo(token, expMillis, jti);
    }

    public Jws<Claims> parse(String token) {
        return Jwts.parserBuilder()
                .requireIssuer(jwtProperties.getIssuer())
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token);
    }

    private byte[] resolveSecret(String secret) {
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("jwt.secret 不能为空");
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("jwt.secret 长度至少 32 字符");
        }
        return bytes;
    }

    public static final class TokenInfo {
        private final String token;
        private final long expiresAtMillis;
        private final String jti;

        public TokenInfo(String token, long expiresAtMillis, String jti) {
            this.token = token;
            this.expiresAtMillis = expiresAtMillis;
            this.jti = jti;
        }

        public String getToken() {
            return token;
        }

        public long getExpiresAtMillis() {
            return expiresAtMillis;
        }

        public String getJti() {
            return jti;
        }
    }
}

