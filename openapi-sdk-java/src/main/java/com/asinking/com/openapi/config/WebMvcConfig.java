package com.asinking.com.openapi.config;

import com.asinking.com.openapi.security.ApiKeyInterceptor;
import com.asinking.com.openapi.interceptor.JwtAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Web MVC 配置：CORS 跨域、JWT 认证拦截器（/api/**）、API Key 拦截器（/api/lingxing/**）。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final FrontendAuthProperties frontendAuthProperties;
    private final ApiKeyInterceptor apiKeyInterceptor;
    private final JwtAuthInterceptor jwtAuthInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;
    private final com.asinking.com.openapi.security.GoodcangSignatureInterceptor goodcangSignatureInterceptor;

    public WebMvcConfig(FrontendAuthProperties frontendAuthProperties, ApiKeyInterceptor apiKeyInterceptor,
                        JwtAuthInterceptor jwtAuthInterceptor, RateLimitInterceptor rateLimitInterceptor,
                        com.asinking.com.openapi.security.GoodcangSignatureInterceptor goodcangSignatureInterceptor) {
        this.frontendAuthProperties = frontendAuthProperties;
        this.apiKeyInterceptor = apiKeyInterceptor;
        this.jwtAuthInterceptor = jwtAuthInterceptor;
        this.rateLimitInterceptor = rateLimitInterceptor;
        this.goodcangSignatureInterceptor = goodcangSignatureInterceptor;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> allowedOrigins = frontendAuthProperties.getAllowedOrigins();
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins.toArray(new String[0]))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Content-Type", "Authorization", "X-Client-Id", "X-Api-Key")
                .exposedHeaders("Content-Type")
                .allowCredentials(false)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 限流拦截器（最高优先级）
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**");
        // JWT 认证拦截器
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/user/login", "/api/lingxing/**", "/api/goodcang/callback/**",
                        "/swagger-ui/**", "/v3/api-docs/**");
        // 谷仓回调签名验证拦截器
        registry.addInterceptor(goodcangSignatureInterceptor)
                .addPathPatterns("/api/goodcang/callback/**");
        // 领星 API Key 拦截器
        registry.addInterceptor(apiKeyInterceptor)
                .addPathPatterns("/api/lingxing/**");
    }
}
