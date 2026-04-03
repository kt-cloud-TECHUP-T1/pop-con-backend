package com.t1.popcon.draw.service;

import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.draw.client.TicketServiceClient;
import com.t1.popcon.draw.client.dto.TicketIssueRequest;
import com.t1.popcon.draw.client.dto.TicketIssueResponse;
import com.t1.popcon.draw.domain.Draw;
import com.t1.popcon.draw.domain.DrawEntry;
import com.t1.popcon.draw.domain.DrawEntryStatus;
import com.t1.popcon.draw.domain.DrawOption;
import com.t1.popcon.draw.dto.response.DrawExecuteResponse;
import com.t1.popcon.draw.dto.response.DrawResultConfirmResponse;
import com.t1.popcon.draw.repository.DrawEntryRepository;
import com.t1.popcon.draw.repository.DrawOptionRepository;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DrawResultService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final List<DrawEntryStatus> PROCESSED_STATUSES = List.of(
        DrawEntryStatus.WINNER,
        DrawEntryStatus.FAILED
    );
    private static final String DRAW_SOURCE_TYPE = "DRAW";

    private final DrawOptionRepository drawOptionRepository;
    private final DrawEntryRepository drawEntryRepository;
    private final TicketServiceClient ticketServiceClient;
    private final Clock clock;

    @Transactional
    public DrawExecuteResponse executeDraw(Long drawOptionId) {
        DrawOption drawOption = drawOptionRepository.findByIdForUpdate(drawOptionId)
            .orElseThrow(() -> new CustomException(ErrorCode.DRAW_OPTION_NOT_FOUND));
        Draw draw = drawOption.getDraw();
        validateDrawReady(draw);
        validateNotProcessed(drawOption);

        List<DrawEntry> entries = drawEntryRepository.findAllByDrawOption_IdAndStatusOrderByIdAsc(
            drawOptionId,
            DrawEntryStatus.APPLIED
        );

        if (entries.isEmpty()) {
            drawOption.markProcessed(LocalDateTime.now(clock));
            return DrawExecuteResponse.builder()
                .drawId(draw.getId())
                .drawOptionId(drawOptionId)
                .appliedCount(0)
                .winnerCount(0)
                .failedCount(0)
                .build();
        }

        Collections.shuffle(entries, RANDOM);
        int winnerCount = assignResults(entries, draw.getStockPerOption());
        drawOption.markProcessed(LocalDateTime.now(clock));

        return DrawExecuteResponse.builder()
            .drawId(draw.getId())
            .drawOptionId(drawOptionId)
            .appliedCount(entries.size())
            .winnerCount(winnerCount)
            .failedCount(entries.size() - winnerCount)
            .build();
    }

    @Transactional
    public DrawResultConfirmResponse confirmResult(Long userId, Long drawEntryId) {
        DrawEntry entry = drawEntryRepository.findByIdAndUserId(drawEntryId, userId)
            .orElseThrow(() -> new CustomException(ErrorCode.DRAW_ENTRY_NOT_FOUND));

        validateConfirmable(entry);

        Draw draw = entry.getDrawOption().getDraw();
        LocalDateTime now = LocalDateTime.now(clock);
        entry.markResultChecked(now);

        TicketIssueResponse ticket = issueDrawTicket(entry, draw);
        entry.markTicketIssued(now);

        return DrawResultConfirmResponse.builder()
            .drawEntryId(entry.getId())
            .drawId(draw.getId())
            .announcementAt(draw.getAnnouncementAt())
            .resultCheckedAt(entry.getResultCheckedAt())
            .ticket(ticket)
            .build();
    }

    private void validateDrawReady(Draw draw) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (!now.isAfter(draw.getDrawCloseAt())) {
            throw new CustomException(ErrorCode.DRAW_RESULT_NOT_READY);
        }
    }

    private void validateNotProcessed(DrawOption drawOption) {
        if (drawOption.isProcessed()
            || drawEntryRepository.existsByDrawOption_IdAndStatusIn(drawOption.getId(), PROCESSED_STATUSES)) {
            throw new CustomException(ErrorCode.DRAW_ALREADY_PROCESSED);
        }
    }

    private int assignResults(List<DrawEntry> entries, int maxWinnerCount) {
        int winnerCount = Math.min(maxWinnerCount, entries.size());

        for (int i = 0; i < entries.size(); i++) {
            if (i < winnerCount) {
                entries.get(i).markAsWinner();
            } else {
                entries.get(i).markAsFailed();
            }
        }

        return winnerCount;
    }

    private void validateConfirmable(DrawEntry entry) {
        if (entry.getStatus() == DrawEntryStatus.APPLIED) {
            throw new CustomException(ErrorCode.DRAW_RESULT_NOT_READY);
        }

        Draw draw = entry.getDrawOption().getDraw();
        LocalDateTime now = LocalDateTime.now(clock);
        if (now.isBefore(draw.getAnnouncementAt())) {
            throw new CustomException(ErrorCode.DRAW_RESULT_NOT_ANNOUNCED);
        }

        if (entry.getStatus() != DrawEntryStatus.WINNER) {
            throw new CustomException(ErrorCode.DRAW_NOT_WINNER);
        }
    }

    private TicketIssueResponse issueDrawTicket(DrawEntry entry, Draw draw) {
        try {
            ApiResponse<TicketIssueResponse> response = ticketServiceClient.issueTicket(
                new TicketIssueRequest(
                    entry.getUserId(),
                    draw.getPopupId(),
                    DRAW_SOURCE_TYPE,
                    entry.getId(),
                    null,
                    entry.getDrawOption().getEntryDate(),
                    entry.getDrawOption().getEntryTime()
                )
            );

            if (response == null || response.getData() == null) {
                throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR);
            }

            return response.getData();
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("Draw ticket issuance failed - drawEntryId={}, error={}", entry.getId(), e.getMessage());
            throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR, e);
        }
    }
}
