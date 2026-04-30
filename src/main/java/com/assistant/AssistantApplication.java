package com.assistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AI 知识库助手启动类
 *
 * @SpringBootApplication 是一个组合注解，包含：
 * - @Configuration: 标识这是一个配置类
 * - @EnableAutoConfiguration: 启用 Spring Boot 自动配置
 * - @ComponentScan: 自动扫描当前包及子包下的组件
 */
@SpringBootApplication
public class AssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(AssistantApplication.class, args);
    }
}