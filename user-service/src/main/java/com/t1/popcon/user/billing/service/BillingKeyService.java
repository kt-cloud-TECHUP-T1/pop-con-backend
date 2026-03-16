package com.t1.popcon.user.billing.service;

import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.user.billing.dto.BillingKeyInfoResponse;
import com.t1.popcon.user.billing.dto.BillingKeyRegisterRequest;
import com.t1.popcon.user.billing.dto.PortoneBillingKeyResponse;
import com.t1.popcon.user.billing.entity.UserBillingKey;
import com.t1.popcon.user.billing.infrastructure.PortoneClient;
import com.t1.popcon.user.billing.repository.UserBillingKeyRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BillingKeyService {

	private final UserBillingKeyRepository billingKeyRepository;
	private final PortoneClient portoneClient;

	@Transactional
	public BillingKeyInfoResponse registerBillingKey(Long userId, BillingKeyRegisterRequest request) {
		// 1. 기존 카드 비활성화
		billingKeyRepository.findByUserIdAndIsActiveTrue(userId)
			.ifPresent(UserBillingKey::deactivate);

		// 2. 포트원 상세 조회
		String cleanedUid = request.customerUid().trim();
		PortoneBillingKeyResponse response = portoneClient.fetchBillingKeyInfo(cleanedUid);

		// 3. 응답 검증 (포트원 응답의 status가 ISSUED가 아니면 에러 처리)
		if (response == null || !"ISSUED".equals(response.status())) {
			throw new CustomException(ErrorCode.PAYMENT_FETCH_FAILED, "유효하지 않은 빌링키 상태입니다.");
		}

		// 4. DB 저장
		UserBillingKey newBillingKey = UserBillingKey.builder()
			.userId(userId)
			.customerUid(response.billingKey())
			.pgProvider(response.getPgProvider())
			.cardName(response.getCardName())
			.cardNumber(response.getCardNumber())
			.build();

		return BillingKeyInfoResponse.from(billingKeyRepository.save(newBillingKey));
	}
}