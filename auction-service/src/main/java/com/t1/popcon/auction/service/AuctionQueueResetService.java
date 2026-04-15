// 테스트용 경매 대기열 Redis 초기화 서비스
package com.t1.popcon.auction.service;

import com.t1.popcon.queue.common.redis.QueueRedisKeys;
import java.nio.charset.StandardCharsets;
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
public class AuctionQueueResetService {

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

    // SCAN으로 패턴 매칭 키 수집 후 일괄 삭제
    private void scanAndDelete(String pattern) {
        // 1. 검색 옵션: 패턴에 맞는 키를 한 번에 200개씩 가져오도록 설정 (부하 방지)
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(200).build();
        List<String> keys = new ArrayList<>();

        // 2. Redis 하위 커넥션을 열고 Cursor를 이용해 키를 순회하며 List에 수집
        redisTemplate.execute((RedisCallback<Void>) (RedisConnection connection) -> {
            try (Cursor<byte[]> cursor = connection.keyCommands().scan(options)) {
                // 바이트 배열로 넘어온 키 값을 UTF-8 문자열로 변환하여 저장
                cursor.forEachRemaining(k -> keys.add(new String(k, StandardCharsets.UTF_8)));
            }
            return null;
        });

        // 3. 수집된 키가 존재할 경우 한 번에 일괄 삭제 처리
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info(">>>> [TestReset] 패턴={} 키 {}개 삭제", pattern, keys.size());
        }
    }
}
