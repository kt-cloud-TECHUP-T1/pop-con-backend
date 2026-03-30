package com.t1.popcon.worker.config;

import com.t1.popcon.queue.common.config.QueueProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * queue-worker 설정
 * - QueueProperties: queue.* yml 바인딩 활성화
 */
@Configuration
@EnableConfigurationProperties(QueueProperties.class)
public class WorkerConfig {
}
