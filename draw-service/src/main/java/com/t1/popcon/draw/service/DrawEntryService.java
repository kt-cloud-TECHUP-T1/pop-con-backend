package com.t1.popcon.draw.service;

import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.draw.client.PopupServiceClient;
import com.t1.popcon.draw.client.UserServiceClient;
import com.t1.popcon.draw.client.dto.PopupInternalResponse;
import com.t1.popcon.draw.client.dto.UserInternalResponse;
import com.t1.popcon.draw.domain.Draw;
import com.t1.popcon.draw.domain.DrawEntry;
import com.t1.popcon.draw.domain.DrawEntryStatus;
import com.t1.popcon.draw.domain.DrawOption;
import com.t1.popcon.draw.dto.request.DrawEntryRequest;
import com.t1.popcon.draw.dto.response.DrawEntryResponse;
import com.t1.popcon.draw.repository.DrawEntryRepository;
import com.t1.popcon.draw.repository.DrawOptionRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DrawEntryService {

    private final DrawEntryRepository drawEntryRepository;
    private final DrawOptionRepository drawOptionRepository;
    private final PopupServiceClient popupServiceClient;
    private final UserServiceClient userServiceClient;
    private final Clock clock;

    @Transactional
    public void applyForDraw(Long userId, Long drawId, Long drawOptionId, DrawEntryRequest request) {
        if (!request.isTermsAgreed() || !request.isPrivacyAgreed()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        DrawOption drawOption = drawOptionRepository.findById(drawOptionId)
            .orElseThrow(() -> new CustomException(ErrorCode.DRAW_OPTION_NOT_FOUND));

        if (!drawOption.getDraw().getId().equals(drawId)) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        if (now.isBefore(drawOption.getDraw().getDrawOpenAt())) {
            throw new CustomException(ErrorCode.DRAW_NOT_OPEN);
        }
        if (now.isAfter(drawOption.getDraw().getDrawCloseAt())) {
            throw new CustomException(ErrorCode.DRAW_ALREADY_CLOSED);
        }

        if (drawEntryRepository.existsByUserIdAndDrawOption_Id(userId, drawOptionId)) {
            throw new CustomException(ErrorCode.DRAW_ALREADY_APPLIED);
        }

        ApiResponse<UserInternalResponse> response = userServiceClient.getUserInternal(userId);
        UserInternalResponse userInfo = response != null ? response.getData() : null;
        if (userInfo == null) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        DrawEntry entry = DrawEntry.builder()
            .userId(userId)
            .drawOption(drawOption)
            .encryptedName(userInfo.encryptedName())
            .encryptedPhoneNumber(userInfo.encryptedPhoneNumber())
            .isTermsAgreed(request.isTermsAgreed())
            .isPrivacyAgreed(request.isPrivacyAgreed())
            .build();

        try {
            drawEntryRepository.save(entry);
        } catch (DataIntegrityViolationException e) {
            if (isDuplicateEntryViolation(e)) {
                throw new CustomException(ErrorCode.DRAW_ALREADY_APPLIED);
            }
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public Slice<DrawEntryResponse> getEntriesByUserId(Long userId, Pageable pageable) {
        Slice<DrawEntry> entries = drawEntryRepository.findAllByUserIdOrderByCreatedAtDesc(userId, pageable);

        List<Long> popupIds = entries.getContent().stream()
            .map(entry -> entry.getDrawOption().getDraw().getPopupId())
            .distinct()
            .toList();

        Map<Long, PopupInternalResponse> popupMap = fetchPopupsInBatch(popupIds);

        return entries.map(entry -> {
            PopupInternalResponse popupInfo = popupMap.get(entry.getDrawOption().getDraw().getPopupId());
            return convertToResponse(entry, popupInfo);
        });
    }

    private Map<Long, PopupInternalResponse> fetchPopupsInBatch(List<Long> popupIds) {
        if (popupIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            ApiResponse<List<PopupInternalResponse>> response = popupServiceClient.getPopupsByBulkIds(popupIds);
            if (response == null || response.getData() == null) {
                throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR);
            }
            return response.getData().stream()
                .collect(Collectors.toMap(PopupInternalResponse::popupId, popup -> popup));
        } catch (Exception e) {
            log.error("Popup batch fetch failed - popupIds={}, error={}", popupIds, e.getMessage());
            throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR);
        }
    }

    private DrawEntryResponse convertToResponse(DrawEntry entry, PopupInternalResponse popupInfo) {
        LocalDateTime now = LocalDateTime.now(clock);
        Draw draw = entry.getDrawOption().getDraw();
        LocalDateTime announcementAt = draw.getAnnouncementAt();
        boolean resultAvailable = !now.isBefore(announcementAt);
        boolean resultChecked = entry.getResultCheckedAt() != null;

        String displayStatus = resolveDisplayStatus(entry, now, announcementAt);
        boolean clickable = entry.getStatus() == DrawEntryStatus.WINNER && resultChecked;

        return DrawEntryResponse.builder()
            .id(entry.getId())
            .drawId(draw.getId())
            .thumbnailUrl(popupInfo != null ? popupInfo.thumbnailUrl() : null)
            .title(popupInfo != null ? popupInfo.title() : "알 수 없는 팝업")
            .price(0L)
            .paidAt(entry.getPaidAt())
            .displayStatus(displayStatus)
            .status(entry.getStatus().name())
            .announcementAt(announcementAt)
            .resultAvailable(resultAvailable)
            .resultChecked(resultChecked)
            .clickable(clickable)
            .build();
    }

    private String resolveDisplayStatus(DrawEntry entry, LocalDateTime now, LocalDateTime announcementAt) {
        if (entry.getStatus() == DrawEntryStatus.APPLIED) {
            return now.isBefore(entry.getDrawOption().getDraw().getDrawCloseAt()) ? "진행중" : "추첨 대기";
        }

        if (now.isBefore(announcementAt)) {
            return "결과 발표 대기";
        }

        if (entry.getStatus() == DrawEntryStatus.WINNER) {
            return entry.getResultCheckedAt() == null ? "당첨" : "티켓 발급 완료";
        }

        return entry.getStatus().getDescription();
    }

    private boolean isDuplicateEntryViolation(DataIntegrityViolationException e) {
        Throwable root = e.getMostSpecificCause();
        String message = root != null ? root.getMessage() : e.getMessage();
        return message != null && message.contains("uk_user_draw_option");
    }
}
