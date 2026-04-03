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
import feign.FeignException;
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

    private static final String UNKNOWN_POPUP_TITLE = "알 수 없는 팝업";
    private static final long DEFAULT_PRICE = 0L;
    private static final String DISPLAY_IN_PROGRESS = "진행중";
    private static final String DISPLAY_DRAW_PENDING = "추첨 대기";
    private static final String DISPLAY_ANNOUNCEMENT_PENDING = "결과 발표 대기";
    private static final String DISPLAY_TICKET_ISSUED = "티켓 발급 완료";

    private final DrawEntryRepository drawEntryRepository;
    private final DrawOptionRepository drawOptionRepository;
    private final PopupServiceClient popupServiceClient;
    private final UserServiceClient userServiceClient;
    private final Clock clock;

    @Transactional
    public void applyForDraw(Long userId, Long drawId, Long drawOptionId, DrawEntryRequest request) {
        validateAgreements(request);

        DrawOption drawOption = drawOptionRepository.findById(drawOptionId)
            .orElseThrow(() -> new CustomException(ErrorCode.DRAW_OPTION_NOT_FOUND));
        validateDrawOption(drawId, drawOption);
        validateEntryWindow(drawOption.getDraw());

        if (drawEntryRepository.existsByUserIdAndDrawOption_Id(userId, drawOptionId)) {
            throw new CustomException(ErrorCode.DRAW_ALREADY_APPLIED);
        }

        UserInternalResponse userInfo = fetchUserInfo(userId);

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
        Map<Long, PopupInternalResponse> popupMap = fetchPopupsInBatch(extractPopupIds(entries));

        return entries.map(entry -> convertToResponse(
            entry,
            popupMap.get(entry.getDrawOption().getDraw().getPopupId())
        ));
    }

    private void validateAgreements(DrawEntryRequest request) {
        if (!request.isTermsAgreed() || !request.isPrivacyAgreed()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }

    private void validateDrawOption(Long drawId, DrawOption drawOption) {
        if (!drawOption.getDraw().getId().equals(drawId)) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }

    private void validateEntryWindow(Draw draw) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (now.isBefore(draw.getDrawOpenAt())) {
            throw new CustomException(ErrorCode.DRAW_NOT_OPEN);
        }
        if (now.isAfter(draw.getDrawCloseAt())) {
            throw new CustomException(ErrorCode.DRAW_ALREADY_CLOSED);
        }
    }

    private UserInternalResponse fetchUserInfo(Long userId) {
        try {
            ApiResponse<UserInternalResponse> response = userServiceClient.getUserInternal(userId);
            UserInternalResponse userInfo = response != null ? response.getData() : null;
            if (userInfo == null) {
                throw new CustomException(ErrorCode.USER_NOT_FOUND);
            }
            return userInfo;
        } catch (CustomException e) {
            if (e.getErrorCode() == ErrorCode.USER_NOT_FOUND) {
                throw e;
            }
            throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR, e);
        } catch (FeignException e) {
            if (e.status() == 404) {
                throw new CustomException(ErrorCode.USER_NOT_FOUND);
            }
            throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR, e);
        } catch (RuntimeException e) {
            throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR, e);
        }
    }

    private List<Long> extractPopupIds(Slice<DrawEntry> entries) {
        return entries.getContent().stream()
            .map(entry -> entry.getDrawOption().getDraw().getPopupId())
            .distinct()
            .toList();
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
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("Popup batch fetch failed - popupIds={}", popupIds, e);
            throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR, e);
        }
    }

    private DrawEntryResponse convertToResponse(DrawEntry entry, PopupInternalResponse popupInfo) {
        Draw draw = entry.getDrawOption().getDraw();
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime announcementAt = draw.getAnnouncementAt();
        boolean resultChecked = entry.getResultCheckedAt() != null;
        boolean resultAvailable = !now.isBefore(announcementAt) && entry.getStatus() != DrawEntryStatus.APPLIED;
        boolean clickable = entry.getStatus() == DrawEntryStatus.WINNER && resultAvailable && !resultChecked;

        return DrawEntryResponse.builder()
            .id(entry.getId())
            .drawId(draw.getId())
            .thumbnailUrl(popupInfo != null ? popupInfo.vThumbnailUrl() : null)
            .title(popupInfo != null ? popupInfo.title() : UNKNOWN_POPUP_TITLE)
            .price(DEFAULT_PRICE)
            .paidAt(entry.getPaidAt())
            .displayStatus(resolveDisplayStatus(entry, now, announcementAt))
            .status(entry.getStatus().name())
            .announcementAt(announcementAt)
            .resultAvailable(resultAvailable)
            .resultChecked(resultChecked)
            .clickable(clickable)
            .build();
    }

    private String resolveDisplayStatus(DrawEntry entry, LocalDateTime now, LocalDateTime announcementAt) {
        if (entry.getStatus() == DrawEntryStatus.APPLIED) {
            return now.isBefore(entry.getDrawOption().getDraw().getDrawCloseAt())
                ? DISPLAY_IN_PROGRESS
                : DISPLAY_DRAW_PENDING;
        }

        if (now.isBefore(announcementAt)) {
            return DISPLAY_ANNOUNCEMENT_PENDING;
        }

        if (entry.getStatus() == DrawEntryStatus.WINNER) {
            return entry.getResultCheckedAt() == null
                ? DrawEntryStatus.WINNER.getDescription()
                : DISPLAY_TICKET_ISSUED;
        }

        return entry.getStatus().getDescription();
    }

    private boolean isDuplicateEntryViolation(DataIntegrityViolationException e) {
        Throwable root = e.getMostSpecificCause();
        String message = root != null ? root.getMessage() : e.getMessage();
        return message != null && message.contains("uk_user_draw_option");
    }
}
