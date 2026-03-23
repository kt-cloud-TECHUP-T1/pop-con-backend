package com.t1.popcon.draw.service;

import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.draw.domain.DrawEntry;
import com.t1.popcon.draw.domain.DrawOption;
import com.t1.popcon.draw.repository.DrawEntryRepository;
import com.t1.popcon.draw.repository.DrawOptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DrawEntryService {

	private final DrawEntryRepository drawEntryRepository;
	private final DrawOptionRepository drawOptionRepository;

	@Transactional
	public void applyForDraw(Long userId, Long drawOptionId) {
		// 1. 회차(옵션) 정보 조회
		DrawOption drawOption = drawOptionRepository.findById(drawOptionId)
			.orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT));

		// 2. 응모 기간 검증 (현재 시간이 오픈 시간과 클로즈 시간 사이인지)
		LocalDateTime now = LocalDateTime.now();
		if (now.isBefore(drawOption.getDraw().getDrawOpenAt())) {
			throw new CustomException(ErrorCode.DRAW_NOT_OPEN);
		}
		if (now.isAfter(drawOption.getDraw().getDrawCloseAt())) {
			throw new CustomException(ErrorCode.DRAW_ALREADY_CLOSED);
		}

		// 3. 1차 중복 응모 검증
		if (drawEntryRepository.existsByUserIdAndDrawOption_Id(userId, drawOptionId)) {
			throw new CustomException(ErrorCode.DRAW_ALREADY_APPLIED);
		}

		// 4. 응모 내역 생성 및 저장
		DrawEntry entry = DrawEntry.builder()
			.userId(userId)
			.drawOption(drawOption)
			.build();

		try {
			drawEntryRepository.save(entry);
		} catch (DataIntegrityViolationException e) {
			// 5. 동시성 이슈 발생 시 (동시에 여러 요청이 들어와 DB 유니크 제약조건에 걸릴 경우)
			throw new CustomException(ErrorCode.DRAW_ALREADY_APPLIED);
		}
	}
}