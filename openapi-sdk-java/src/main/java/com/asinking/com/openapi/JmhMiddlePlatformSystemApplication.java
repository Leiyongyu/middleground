package com.asinking.com.openapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 聚米汇中台系统启动类，基于 Spring Boot 2.7，对接领星 OpenAPI。
 */
@SpringBootApplication
@EnableScheduling
public class JmhMiddlePlatformSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(JmhMiddlePlatformSystemApplication.class, args);
    }
}
