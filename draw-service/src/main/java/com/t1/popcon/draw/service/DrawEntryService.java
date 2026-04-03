package com.t1.popcon.draw.service;

import com.t1.popcon.common.encryption.EncryptionService;
import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.draw.client.PopupServiceClient;
import com.t1.popcon.draw.client.UserServiceClient;
import com.t1.popcon.draw.client.dto.PopupInternalResponse;
import com.t1.popcon.draw.client.dto.UserInternalResponse;
import com.t1.popcon.draw.domain.DrawEntry;
import com.t1.popcon.draw.domain.DrawEntryStatus;
import com.t1.popcon.draw.domain.DrawOption;
import com.t1.popcon.draw.dto.request.DrawEntryRequest;
import com.t1.popcon.draw.dto.response.DrawEntryResponse;
import com.t1.popcon.draw.dto.response.DrawEntryResultResponse;
import com.t1.popcon.draw.repository.DrawEntryRepository;
import com.t1.popcon.draw.repository.DrawOptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DrawEntryService {

	private final DrawEntryRepository drawEntryRepository;
	private final DrawOptionRepository drawOptionRepository;
	private final PopupServiceClient popupServiceClient;
	private final UserServiceClient userServiceClient;
	private final EncryptionService encryptionService;

	@Transactional
	public DrawEntryResultResponse applyForDraw(Long userId, Long drawId, Long drawOptionId, DrawEntryRequest request) {
		// 1. 기본 입력 검증 (약관 동의)
		if (!request.isTermsAgreed() || !request.isPrivacyAgreed()) {
			throw new CustomException(ErrorCode.INVALID_INPUT);
		}

		// 2. 회차(옵션) 정보 조회
		DrawOption drawOption = drawOptionRepository.findById(drawOptionId)
			.orElseThrow(() -> new CustomException(ErrorCode.DRAW_OPTION_NOT_FOUND));

		// 3. 경로상의 drawId와 옵션의 drawId가 일치하는지 검증
		if (!drawOption.getDraw().getId().equals(drawId)) {
			throw new CustomException(ErrorCode.INVALID_INPUT);
		}

		// 4. 응모 기간 검증 (현재 시간이 오픈 시간과 클로즈 시간 사이인지)
		LocalDateTime now = LocalDateTime.now();
		if (now.isBefore(drawOption.getDraw().getDrawOpenAt())) {
			throw new CustomException(ErrorCode.DRAW_NOT_OPEN);
		}
		if (now.isAfter(drawOption.getDraw().getDrawCloseAt())) {
			throw new CustomException(ErrorCode.DRAW_ALREADY_CLOSED);
		}

		// 5. 1차 중복 응모 검증 (DB 조회)
		if (drawEntryRepository.existsByUserIdAndDrawOption_Id(userId, drawOptionId)) {
			throw new CustomException(ErrorCode.DRAW_ALREADY_APPLIED);
		}

		// 6. 사용자 정보 조회 (암호화된 이름, 전화번호)
		UserInternalResponse userInfo = userServiceClient.getUserInternal(userId).getData();
		if (userInfo == null) {
			throw new CustomException(ErrorCode.USER_NOT_FOUND);
		}

		// 7. 응모 내역 생성 및 저장
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

		// 8. 응모 성공 상세 정보 조회 및 조합
		ApiResponse<PopupInternalResponse> popupResponse = popupServiceClient.getPopupDetail(drawOption.getDraw().getPopupId());
		PopupInternalResponse popupInfo = (popupResponse != null) ? popupResponse.getData() : null;

		return DrawEntryResultResponse.builder()
			.vThumbnailUrl(popupInfo != null ? popupInfo.vThumbnailUrl() : null)
			.popupTitle(popupInfo != null ? popupInfo.title() : "알 수 없는 팝업")
			.popupAddress(popupInfo != null ? popupInfo.location() : null)
			.entryDate(drawOption.getEntryDate())
			.entryTime(drawOption.getEntryTime())
			.userName(encryptionService.decrypt(userInfo.encryptedName()))
			.userPhoneNumber(encryptionService.decrypt(userInfo.encryptedPhoneNumber()))
			.build();
	}

	@Transactional(readOnly = true)
	public Slice<DrawEntryResponse> getEntriesByUserId(Long userId, Pageable pageable) {
		Slice<DrawEntry> entries = drawEntryRepository.findAllByUserIdOrderByCreatedAtDesc(userId, pageable);

		// 1. 필요한 모든 popupId 추출
		List<Long> popupIds = entries.getContent().stream()
			.map(entry -> entry.getDrawOption().getDraw().getPopupId())
			.distinct()
			.collect(Collectors.toList());

		// 2. 팝업 정보 배치 조회 (N+1 방지)
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
				.collect(Collectors.toMap(PopupInternalResponse::popupId, responseDto -> responseDto));
		} catch (Exception e) {
			log.error("팝업 정보 배치 조회 실패 - popupIds: {}, error: {}", popupIds, e.getMessage());
			throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR);
		}
	}

	private DrawEntryResponse convertToResponse(DrawEntry entry, PopupInternalResponse popupInfo) {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime drawCloseAt = entry.getDrawOption().getDraw().getDrawCloseAt();
		String displayStatus;

		if (entry.getStatus() == DrawEntryStatus.APPLIED) {
			if (now.isBefore(drawCloseAt)) {
				displayStatus = "진행중";
			} else {
				displayStatus = "응모 완료";
			}
		} else {
			displayStatus = entry.getStatus().getDescription(); // "당첨" 또는 "미당첨"
		}

		return DrawEntryResponse.builder()
			.id(entry.getId())
			.drawId(entry.getDrawOption().getDraw().getId())
			.vThumbnailUrl(popupInfo != null ? popupInfo.vThumbnailUrl() : null)
			.title(popupInfo != null ? popupInfo.title() : "알 수 없는 팝업")
			.price(0L) // TODO: 팝업 서비스에서 가격 정보를 주면 연결 필요
			.paidAt(entry.getPaidAt())
			.displayStatus(displayStatus)
			.status(entry.getStatus().name())
			.build();
	}

	private boolean isDuplicateEntryViolation(DataIntegrityViolationException e) {
		Throwable root = e.getMostSpecificCause();
		String message = (root != null ? root.getMessage() : e.getMessage());
		return message != null && message.contains("uk_user_draw_option");
	}
}
