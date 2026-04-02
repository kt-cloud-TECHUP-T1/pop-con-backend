package com.t1.popcon.draw.scheduler;

import com.t1.popcon.draw.domain.DrawEntryStatus;
import com.t1.popcon.draw.domain.DrawOption;
import com.t1.popcon.draw.repository.DrawEntryRepository;
import com.t1.popcon.draw.repository.DrawOptionRepository;
import com.t1.popcon.draw.service.DrawResultService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DrawAutoExecutionScheduler {

    private final DrawOptionRepository drawOptionRepository;
    private final DrawEntryRepository drawEntryRepository;
    private final DrawResultService drawResultService;
    private final Clock clock;

    @Scheduled(cron = "${draw.auto-execute.cron:0 */1 * * * *}")
    public void executeClosedDrawOptions() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<DrawOption> closedOptions = drawOptionRepository.findDistinctByDraw_DrawCloseAtBefore(now);

        for (DrawOption drawOption : closedOptions) {
            Long optionId = drawOption.getId();

            if (drawEntryRepository.existsByDrawOption_IdAndStatusIn(
                optionId,
                List.of(DrawEntryStatus.WINNER, DrawEntryStatus.FAILED)
            )) {
                continue;
            }

            if (!drawEntryRepository.existsByDrawOption_IdAndStatus(optionId, DrawEntryStatus.APPLIED)) {
                continue;
            }

            try {
                drawResultService.executeDraw(optionId);
                log.info("Auto draw execution completed - drawOptionId={}", optionId);
            } catch (Exception e) {
                log.error("Auto draw execution failed - drawOptionId={}, error={}", optionId, e.getMessage());
            }
        }
    }
}
