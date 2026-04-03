package com.t1.popcon.draw.scheduler;

import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.draw.domain.DrawOption;
import com.t1.popcon.draw.repository.DrawOptionRepository;
import com.t1.popcon.draw.service.DrawResultService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DrawAutoExecutionScheduler {

    private static final long LOCK_WAIT_SECONDS = 1L;
    private static final long LOCK_LEASE_SECONDS = 30L;

    private final DrawOptionRepository drawOptionRepository;
    private final DrawResultService drawResultService;
    private final RedissonClient redissonClient;
    private final Clock clock;

    @Scheduled(cron = "${draw.auto-execute.cron:0 */1 * * * *}")
    public void executeClosedDrawOptions() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<DrawOption> closedOptions = drawOptionRepository.findDistinctByDraw_DrawCloseAtBeforeAndProcessedFalse(now);

        for (DrawOption drawOption : closedOptions) {
            executeWithLock(drawOption.getId());
        }
    }

    private void executeWithLock(Long optionId) {
        RLock lock = redissonClient.getLock("draw-option:" + optionId);
        boolean locked = false;

        try {
            locked = lock.tryLock(LOCK_WAIT_SECONDS, LOCK_LEASE_SECONDS, TimeUnit.SECONDS);
            if (!locked) {
                log.debug("Auto draw execution skipped because lock was not acquired - drawOptionId={}", optionId);
                return;
            }

            DrawOption drawOption = drawOptionRepository.findById(optionId).orElse(null);
            if (drawOption == null || drawOption.isProcessed()) {
                return;
            }

            drawResultService.executeDraw(optionId);
            log.info("Auto draw execution completed - drawOptionId={}", optionId);
        } catch (CustomException e) {
            if (e.getErrorCode() == ErrorCode.DRAW_ALREADY_PROCESSED
                || e.getErrorCode() == ErrorCode.DRAW_RESULT_NOT_READY) {
                log.debug("Auto draw execution skipped - drawOptionId={}, reason={}", optionId, e.getErrorCode().getCode());
                return;
            }
            log.error("Auto draw execution failed - drawOptionId={}", optionId, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Auto draw execution interrupted - drawOptionId={}", optionId, e);
        } catch (Exception e) {
            log.error("Auto draw execution failed - drawOptionId={}", optionId, e);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
