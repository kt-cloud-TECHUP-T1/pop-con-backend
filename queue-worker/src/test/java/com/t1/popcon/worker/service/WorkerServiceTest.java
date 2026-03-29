package com.t1.popcon.worker.service;

import com.t1.popcon.queue.common.config.QueueProperties;
import com.t1.popcon.queue.common.redis.QueueActiveRepository;
import com.t1.popcon.queue.common.redis.QueueCleanupRepository;
import com.t1.popcon.queue.common.redis.QueueWaitingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("WorkerService 테스트")
class WorkerServiceTest {

    @Mock
    private QueueWaitingRepository waitingRepository;

    @Mock
    private QueueActiveRepository activeRepository;

    @Mock
    private QueueCleanupRepository cleanupRepository;

    @Mock
    private QueueProperties queueProperties;

    @Mock
    private QueueProperties.ActiveTtl activeTtl;

    @InjectMocks
    private WorkerService workerService;

    private static final String PHASE_TYPE = "draw";
    private static final long PHASE_ID = 1L;

    @BeforeEach
    void setUp() {
        when(queueProperties.getActiveTtl()).thenReturn(activeTtl);
        when(activeTtl.getDrawSeconds()).thenReturn(900L);
        when(activeTtl.getAuctionSeconds()).thenReturn(3600L);
    }

    @Test
    @DisplayName("promote: 여유 슬롯이 있으면 waiting 사용자를 승격한다")
    void promote_WithAvailableSlots_PromotesWaitingUsers() {
        when(queueProperties.getMaxActiveUsers()).thenReturn(100);
        when(queueProperties.getMaxReleasePerCycle()).thenReturn(10);
        when(activeRepository.getActiveCount(anyString(), anyLong())).thenReturn(50L);
        when(waitingRepository.pollWaitingUsers(anyString(), anyLong(), anyInt()))
                .thenReturn(List.of("1001", "1002"));
        // heartbeat 유효 (Long.MAX_VALUE = 먼 미래 → 만료 아님)
        when(waitingRepository.getHeartbeatScore(anyString(), anyLong(), anyLong()))
                .thenReturn(Long.MAX_VALUE);
        when(activeRepository.getUserHash(anyString(), anyLong(), anyLong()))
                .thenReturn(java.util.Map.of("queueToken", "hashedToken"));

        workerService.promote(PHASE_TYPE, PHASE_ID);

        verify(activeRepository, times(1)).removeExpiredActive(PHASE_TYPE, PHASE_ID);
        verify(activeRepository, times(2)).addToActive(eq(PHASE_TYPE), eq(PHASE_ID), anyLong(), anyLong());
        verify(activeRepository, times(2)).updateUserStatus(eq(PHASE_TYPE), eq(PHASE_ID), anyLong(), eq("ACTIVE"));
    }

    @Test
    @DisplayName("promote: heartbeat 만료 사용자는 승격을 건너뛰고 정리한다")
    void promote_WithExpiredHeartbeat_SkipsAndCleansUp() {
        when(queueProperties.getMaxActiveUsers()).thenReturn(100);
        when(queueProperties.getMaxReleasePerCycle()).thenReturn(10);
        when(activeRepository.getActiveCount(anyString(), anyLong())).thenReturn(50L);
        when(waitingRepository.pollWaitingUsers(anyString(), anyLong(), anyInt()))
                .thenReturn(List.of("1001", "1002"));
        // 1001은 heartbeat 만료 (0 = 과거), 1002는 유효 (Long.MAX_VALUE = 먼 미래)
        when(waitingRepository.getHeartbeatScore(anyString(), anyLong(), eq(1001L)))
                .thenReturn(0L);
        when(waitingRepository.getHeartbeatScore(anyString(), anyLong(), eq(1002L)))
                .thenReturn(Long.MAX_VALUE);
        when(activeRepository.getUserHash(anyString(), anyLong(), anyLong()))
                .thenReturn(java.util.Map.of("queueToken", "hashedToken"));

        workerService.promote(PHASE_TYPE, PHASE_ID);

        // 1001은 스킵 → cleanupWaitingUserData 호출
        verify(cleanupRepository, times(1)).cleanupWaitingUserData(eq(PHASE_TYPE), eq(PHASE_ID), eq(1001L), isNull());
        // 1002만 승격
        verify(activeRepository, times(1)).addToActive(eq(PHASE_TYPE), eq(PHASE_ID), eq(1002L), anyLong());
        verify(activeRepository, never()).addToActive(eq(PHASE_TYPE), eq(PHASE_ID), eq(1001L), anyLong());
    }

    @Test
    @DisplayName("promote: 여유 슬롯이 없으면 승격을 건너뛴다")
    void promote_WithNoAvailableSlots_SkipsPromotion() {
        when(queueProperties.getMaxActiveUsers()).thenReturn(100);
        when(activeRepository.getActiveCount(anyString(), anyLong())).thenReturn(100L);

        workerService.promote(PHASE_TYPE, PHASE_ID);

        verify(waitingRepository, never()).pollWaitingUsers(anyString(), anyLong(), anyInt());
    }

    @Test
    @DisplayName("promote: waiting이 비어있으면 승격을 건너뛴다")
    void promote_WithEmptyWaiting_SkipsPromotion() {
        when(queueProperties.getMaxActiveUsers()).thenReturn(100);
        when(activeRepository.getActiveCount(anyString(), anyLong())).thenReturn(50L);
        when(waitingRepository.pollWaitingUsers(anyString(), anyLong(), anyInt()))
                .thenReturn(List.of());

        workerService.promote(PHASE_TYPE, PHASE_ID);

        verify(activeRepository, never()).addToActive(anyString(), anyLong(), anyLong(), anyLong());
    }

    @Test
    @DisplayName("cleanupExpiredActive: 만료된 ACTIVE를 정리한다")
    void cleanupExpiredActive_RemovesExpiredActive() {
        when(activeRepository.removeExpiredActive(anyString(), anyLong())).thenReturn(5L);

        workerService.cleanupExpiredActive(PHASE_TYPE, PHASE_ID);

        verify(activeRepository, times(1)).removeExpiredActive(PHASE_TYPE, PHASE_ID);
    }

    @Test
    @DisplayName("cleanupExpiredHeartbeats: 만료된 heartbeat 사용자를 정리한다")
    void cleanupExpiredHeartbeats_RemovesExpiredUsers() {
        when(waitingRepository.removeExpiredHeartbeatUsers(anyString(), anyLong(), anyLong()))
                .thenReturn(Set.of("1001", "1002", "1003"));
        when(activeRepository.getUserHash(anyString(), anyLong(), anyLong()))
                .thenReturn(java.util.Map.of("queueToken", "hashedToken"));

        workerService.cleanupExpiredHeartbeats(PHASE_TYPE, PHASE_ID);

        verify(cleanupRepository, times(3)).cleanupWaitingUserData(anyString(), anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("cleanupExpiredHeartbeats: 만료 heartbeat가 없으면 정리를 건너뛴다")
    void cleanupExpiredHeartbeats_WithNoExpired_SkipsCleanup() {
        when(waitingRepository.removeExpiredHeartbeatUsers(anyString(), anyLong(), anyLong()))
                .thenReturn(Set.of());

        workerService.cleanupExpiredHeartbeats(PHASE_TYPE, PHASE_ID);

        verify(cleanupRepository, never()).cleanupWaitingUserData(anyString(), anyLong(), anyLong(), any());
    }
}
