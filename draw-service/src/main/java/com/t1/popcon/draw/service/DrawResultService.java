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
import java.time.Clock;
import java.time.LocalDateTime;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DrawResultService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final DrawOptionRepository drawOptionRepository;
    private final DrawEntryRepository drawEntryRepository;
    private final TicketServiceClient ticketServiceClient;
    private final Clock clock;

    @Transactional
    public DrawExecuteResponse executeDraw(Long drawOptionId) {
        DrawOption drawOption = drawOptionRepository.findById(drawOptionId)
            .orElseThrow(() -> new CustomException(ErrorCode.DRAW_OPTION_NOT_FOUND));

        Draw draw = drawOption.getDraw();
        LocalDateTime now = LocalDateTime.now(clock);
        if (!now.isAfter(draw.getDrawCloseAt())) {
            throw new CustomException(ErrorCode.DRAW_RESULT_NOT_READY);
        }

        if (drawEntryRepository.existsByDrawOption_IdAndStatusIn(
            drawOptionId,
            List.of(DrawEntryStatus.WINNER, DrawEntryStatus.FAILED)
        )) {
            throw new CustomException(ErrorCode.DRAW_ALREADY_PROCESSED);
        }

        List<DrawEntry> entries = drawEntryRepository.findAllByDrawOption_IdAndStatusOrderByIdAsc(
            drawOptionId,
            DrawEntryStatus.APPLIED
        );

        Collections.shuffle(entries, RANDOM);

        int winnerCount = Math.min(draw.getStockPerOption(), entries.size());
        for (int i = 0; i < entries.size(); i++) {
            if (i < winnerCount) {
                entries.get(i).markAsWinner();
            } else {
                entries.get(i).markAsFailed();
            }
        }

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

        if (entry.getStatus() == DrawEntryStatus.APPLIED) {
            throw new CustomException(ErrorCode.DRAW_RESULT_NOT_READY);
        }

        Draw draw = entry.getDrawOption().getDraw();
        LocalDateTime announcementAt = draw.getAnnouncementAt();
        LocalDateTime now = LocalDateTime.now(clock);
        if (now.isBefore(announcementAt)) {
            throw new CustomException(ErrorCode.DRAW_RESULT_NOT_ANNOUNCED);
        }

        if (entry.getStatus() != DrawEntryStatus.WINNER) {
            throw new CustomException(ErrorCode.DRAW_NOT_WINNER);
        }

        entry.markResultChecked();

        ApiResponse<TicketIssueResponse> response = ticketServiceClient.issueTicket(
            new TicketIssueRequest(
                entry.getUserId(),
                draw.getPopupId(),
                "DRAW",
                entry.getId(),
                null,
                entry.getDrawOption().getEntryDate(),
                entry.getDrawOption().getEntryTime()
            )
        );

        if (response == null || response.getData() == null) {
            throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR);
        }

        entry.markTicketIssued();

        return DrawResultConfirmResponse.builder()
            .drawEntryId(entry.getId())
            .drawId(draw.getId())
            .announcementAt(announcementAt)
            .resultCheckedAt(entry.getResultCheckedAt())
            .ticket(response.getData())
            .build();
    }
}
