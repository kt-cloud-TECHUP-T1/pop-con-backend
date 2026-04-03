package com.t1.popcon;

import com.t1.popcon.common.auth.config.JpaConfig;
import com.t1.popcon.common.infrastructure.portone.PortOneHttpClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * queue-service 애플리케이션 진입점
 *
 * common 모듈에서 queue-service가 사용하지 않는 빈을 스캔 제외:
 * - JpaConfig: JPA 미사용, @EnableJpaAuditing 로딩 시 spring-aspects 오류 발생
 * - PortOneHttpClient: 결제 인프라 불필요
 */
@EnableFeignClients
@SpringBootApplication
@ComponentScan(
    basePackages = "com.t1.popcon",
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {JpaConfig.class, PortOneHttpClient.class}
    )
)
public class QueueApplication {
    public static void main(String[] args) {
        SpringApplication.run(QueueApplication.class, args);
    }
}
