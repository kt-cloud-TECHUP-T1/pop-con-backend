package com.t1.popcon;

import com.t1.popcon.common.auth.config.JpaConfig;
import com.t1.popcon.common.infrastructure.portone.PortOneHttpClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 대기열 Worker 애플리케이션
 * - 승격/만료정리/heartbeat 스케줄러 전용 (web 서버 없음)
 * - JPA 미사용 → JpaConfig 제외, 포트원 미사용 → PortOneHttpClient 제외
 */
@SpringBootApplication
@EnableScheduling
@ComponentScan(
    basePackages = "com.t1.popcon",
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaConfig.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = PortOneHttpClient.class)
    }
)
public class QueueWorkerApplication {
    public static void main(String[] args) {
        SpringApplication.run(QueueWorkerApplication.class, args);
    }
}
