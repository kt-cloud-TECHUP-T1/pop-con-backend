package com.t1.popcon.worker.redis;

import com.t1.popcon.queue.common.redis.QueuePhaseScanner;
import com.t1.popcon.queue.common.redis.QueuePhaseScanner.PhaseKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("QueuePhaseScanner 테스트")
class QueuePhaseScannerTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @InjectMocks
    private QueuePhaseScanner queuePhaseScanner;

    @Test
    @DisplayName("scanActivePhases: 활성 phase 목록을 스캔한다")
    void scanActivePhases_ReturnsActivePhases() {
        Set<String> waitingKeys = new HashSet<>();
        waitingKeys.add("queue:waiting:draw:1");
        waitingKeys.add("queue:waiting:draw:2");
        waitingKeys.add("queue:waiting:auction:1");

        Set<String> activeKeys = new HashSet<>();
        activeKeys.add("queue:active:draw:1");

        Set<String> heartbeatKeys = new HashSet<>();
        heartbeatKeys.add("queue:heartbeat:auction:1");

        @SuppressWarnings("unchecked")
        Cursor<String> waitingCursor = mock(Cursor.class);
        @SuppressWarnings("unchecked")
        Cursor<String> activeCursor = mock(Cursor.class);
        @SuppressWarnings("unchecked")
        Cursor<String> heartbeatCursor = mock(Cursor.class);

        when(waitingCursor.hasNext()).thenReturn(true, true, true, false);
        when(waitingCursor.next())
                .thenReturn("queue:waiting:draw:1")
                .thenReturn("queue:waiting:draw:2")
                .thenReturn("queue:waiting:auction:1");

        when(activeCursor.hasNext()).thenReturn(true, false);
        when(activeCursor.next()).thenReturn("queue:active:draw:1");

        when(heartbeatCursor.hasNext()).thenReturn(true, false);
        when(heartbeatCursor.next()).thenReturn("queue:heartbeat:auction:1");

        when(redisTemplate.scan(any(ScanOptions.class)))
                .thenReturn(waitingCursor)
                .thenReturn(activeCursor)
                .thenReturn(heartbeatCursor);

        Set<PhaseKey> result = queuePhaseScanner.scanActivePhases();

        assertThat(result).hasSize(3);
        assertThat(result).contains(
                new PhaseKey("draw", 1),
                new PhaseKey("draw", 2),
                new PhaseKey("auction", 1)
        );
    }

    @Test
    @DisplayName("scanActivePhases: 키가 없으면 빈 집합을 반환한다")
    void scanActivePhases_WithNoKeys_ReturnsEmptySet() {
        @SuppressWarnings("unchecked")
        Cursor<String> emptyCursor = mock(Cursor.class);
        when(emptyCursor.hasNext()).thenReturn(false);
        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(emptyCursor);

        Set<PhaseKey> result = queuePhaseScanner.scanActivePhases();

        assertThat(result).isEmpty();
    }
}
