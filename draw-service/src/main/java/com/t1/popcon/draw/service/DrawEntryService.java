package com.t1.popcon.draw.service;

import com.t1.popcon.common.encryption.EncryptionService;
import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.draw.client.PopupServiceClient;
import com.t1.popcon.draw.client.UserServiceClient;
import com.t1.popcon.draw.client.dto.PopupInternalResponse;
import com.t1.popcon.draw.client.dto.UserInternalResponse;
import com.t1.popcon.draw.domain.DrawEntry;
import com.t1.popcon.draw.domain.DrawOption;
import com.t1.popcon.draw.dto.request.DrawEntryRequest;
import com.t1.popcon.draw.dto.response.DrawEntryResponse;
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
	private final EncryptionService encryptionService;
	private final UserServiceClient userServiceClient;

	@Transactional
	public void applyForDraw(Long userId, Long drawId, Long drawOptionId, DrawEntryRequest request) {
		// 1. 회차(옵션) 정보 조회
		DrawOption drawOption = drawOptionRepository.findById(drawOptionId)
			.orElseThrow(() -> new CustomException(ErrorCode.DRAW_OPTION_NOT_FOUND));

		// 2. 경로상의 drawId와 옵션의 drawId가 일치하는지 검증
		if (!drawOption.getDraw().getId().equals(drawId)) {
			throw new CustomException(ErrorCode.INVALID_INPUT);
		}

		// 3. 응모 기간 검증 (현재 시간이 오픈 시간과 클로즈 시간 사이인지)
		LocalDateTime now = LocalDateTime.now();
		if (now.isBefore(drawOption.getDraw().getDrawOpenAt())) {
			throw new CustomException(ErrorCode.DRAW_NOT_OPEN);
		}
		if (now.isAfter(drawOption.getDraw().getDrawCloseAt())) {
			throw new CustomException(ErrorCode.DRAW_ALREADY_CLOSED);
		}

		// 4. 1차 중복 응모 검증
		if (drawEntryRepository.existsByUserIdAndDrawOption_Id(userId, drawOptionId)) {
			throw new CustomException(ErrorCode.DRAW_ALREADY_APPLIED);
		}

		// 5. 사용자 정보 조회 (암호화된 이름, 전화번호)
		UserInternalResponse userInfo = userServiceClient.getUserInternal(userId).getData();
		if (userInfo == null) {
			throw new CustomException(ErrorCode.USER_NOT_FOUND);
		}

		if (!request.isTermsAgreed() || !request.isPrivacyAgreed()) {
			throw new CustomException(ErrorCode.INVALID_INPUT);
		}
		// 6. 응모 내역 생성 및 저장
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
			throw new CustomException(ErrorCode.DRAW_ALREADY_APPLIED);
		}
	}

	@Transactional(readOnly = true)
	public List<DrawEntryResponse> getEntriesByUserId(Long userId) {
		List<DrawEntry> entries = drawEntryRepository.findAllByUserIdOrderByCreatedAtDesc(userId);

		return entries.stream()
			.map(this::convertToResponse)
			.collect(Collectors.toList());
	}

	private DrawEntryResponse convertToResponse(DrawEntry entry) {
		// 팝업 서비스로부터 제목, 썸네일 정보를 가져옴
		PopupInternalResponse popupInfo = null;
		try {
			popupInfo = popupServiceClient.getPopupDetail(entry.getDrawOption().getDraw().getPopupId()).getData();
		} catch (Exception e) {
			// 팝업 서비스 호출 실패 시 기본값 처리 (또는 로그)
		}

			if (isDuplicateEntryViolation(e)) {
				throw new CustomException(ErrorCode.DRAW_ALREADY_APPLIED);
			}
			throw e;
		}
	}

	private boolean isDuplicateEntryViolation(DataIntegrityViolationException e) {
		Throwable root = e.getMostSpecificCause();
		String message = (root != null ? root.getMessage() : e.getMessage());
		return message != null && message.contains("uk_user_draw_option");
	}

	@Transactional(readOnly = true)
	public Slice<DrawEntryResponse> getEntriesByUserId(Long userId, Pageable pageable) {
		Slice<DrawEntry> entries = drawEntryRepository.findAllByUserIdOrderByCreatedAtDesc(userId, pageable);

		// 1. 필요한 모든 popupId 추출
		List<Long> popupIds = entries.getContent().stream()
			.map(entry -> entry.getDrawOption().getDraw().getPopupId())
			.distinct()
			.collect(Collectors.toList());

		// 2. 팝업 정보 배치 조회
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
			List<PopupInternalResponse> popupList = popupServiceClient.getPopupsByBulkIds(popupIds).getData();
			if (popupList == null) {
				return Collections.emptyMap();
			}
			return popupList.stream()
				.collect(Collectors.toMap(PopupInternalResponse::popupId, response -> response));
		} catch (Exception e) {
			log.warn("팝업 정보 배치 조회 실패 - popupIds: {}", popupIds, e);
			return Collections.emptyMap();
		}
	}

	private DrawEntryResponse convertToResponse(DrawEntry entry, PopupInternalResponse popupInfo) {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime drawCloseAt = entry.getDrawOption().getDraw().getDrawCloseAt();
		String displayStatus;

		if (entry.getStatus() == com.t1.popcon.draw.domain.DrawEntryStatus.APPLIED) {
			if (now.isBefore(drawCloseAt)) {
				displayStatus = "진행중";
			} else {
				displayStatus = "응모 완료";
			}
		} else {
			displayStatus = entry.getStatus().getDescription(); // "당첨" 또는 "미당첨"
		}

		return DrawEntryResponse.builder()
			.thumbnailUrl(popupInfo != null ? popupInfo.thumbnailUrl() : null)
			.title(popupInfo != null ? popupInfo.title() : "알 수 없는 팝업")
			.price(0L) // TODO: 팝업 서비스에서 가격 정보를 주면 연결 필요, 또는 드로우 엔티티에 가격 추가
			.paidAt(entry.getPaidAt())
			.displayStatus(displayStatus)
			.status(entry.getStatus().name())
			.build();
	}
}