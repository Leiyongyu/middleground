package com.asinking.com.openapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 聚米汇中台系统启动类。
 * @EnableAsync 已由 AsyncConfig 统一配置。
 */
@SpringBootApplication
@EnableScheduling
public class JmhMiddlePlatformSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(JmhMiddlePlatformSystemApplication.class, args);
    }
}
