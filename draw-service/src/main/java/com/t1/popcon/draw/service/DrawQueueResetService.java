// 테스트용 드로우 대기열 Redis 초기화 서비스
package com.t1.popcon.draw.service;

import com.t1.popcon.queue.common.redis.QueueRedisKeys;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DrawQueueResetService {

    private final StringRedisTemplate redisTemplate;

    /**
     * phaseType/phaseId에 해당하는 대기열 Redis 키 전체 삭제
     * - 고정 키(waiting, heartbeat, active, seq): DEL
     * - 사용자별 키(user:*, block:*): SCAN → DEL
     */
    public void reset(String phaseType, long phaseId) {
        deleteFixedKeys(phaseType, phaseId);
        scanAndDelete("queue:user:" + phaseType + ":" + phaseId + ":*");
        scanAndDelete("queue:block:" + phaseType + ":" + phaseId + ":*");
        log.info(">>>> [TestReset] {}:{} 대기열 Redis 초기화 완료", phaseType, phaseId);
    }

    // 고정 키 일괄 삭제
    private void deleteFixedKeys(String phaseType, long phaseId) {
        List<String> keys = List.of(
            QueueRedisKeys.waiting(phaseType, phaseId),
            QueueRedisKeys.heartbeat(phaseType, phaseId),
            QueueRedisKeys.active(phaseType, phaseId),
            QueueRedisKeys.seq(phaseType, phaseId)
        );
        redisTemplate.delete(keys);
    }

    private static final int BATCH_SIZE = 1000;

    // SCAN으로 패턴 매칭 키를 배치 단위로 즉시 삭제 (전체 수집 시 OOM 방지)
    private void scanAndDelete(String pattern) {
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(200).build();

        redisTemplate.execute((RedisCallback<Void>) (RedisConnection connection) -> {
            try (Cursor<byte[]> cursor = connection.keyCommands().scan(options)) {
                List<byte[]> batch = new ArrayList<>();
                int totalDeleted = 0;

                while (cursor.hasNext()) {
                    batch.add(cursor.next());

                    // 배치가 가득 차면 즉시 삭제 후 초기화
                    if (batch.size() >= BATCH_SIZE) {
                        connection.keyCommands().del(batch.toArray(new byte[0][]));
                        totalDeleted += batch.size();
                        batch.clear();
                    }
                }

                // 커서 종료 후 남은 키 삭제
                if (!batch.isEmpty()) {
                    connection.keyCommands().del(batch.toArray(new byte[0][]));
                    totalDeleted += batch.size();
                }

                if (totalDeleted > 0) {
                    log.info(">>>> [TestReset] 패턴={} 키 {}개 삭제", pattern, totalDeleted);
                }
            }
            return null;
        });
    }
}
