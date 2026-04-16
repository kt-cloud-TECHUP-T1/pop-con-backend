package com.t1.popcon.queue.config;

import com.t1.popcon.queue.common.config.QueueProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * queue-service 설정
 * - QueueProperties: queue.* yml 바인딩 활성화
 */
@Configuration
@EnableConfigurationProperties(QueueProperties.class)
@EnableScheduling
public class QueueServiceConfig {
}
