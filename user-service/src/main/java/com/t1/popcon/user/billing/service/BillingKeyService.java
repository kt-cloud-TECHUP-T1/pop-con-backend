package com.t1.popcon.user.billing.service;

import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.user.billing.dto.BillingKeyInfoResponse;
import com.t1.popcon.user.billing.dto.BillingKeyRegisterRequest;
import com.t1.popcon.common.infrastructure.dto.PortOneBillingKeyResponse;
import com.t1.popcon.user.billing.entity.UserBillingKey;
import com.t1.popcon.common.infrastructure.portone.PortOneClient;
import com.t1.popcon.user.billing.repository.UserBillingKeyRepository;
import com.t1.popcon.user.domain.User;
import com.t1.popcon.user.repository.UserRepository;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BillingKeyService {

	private final UserBillingKeyRepository billingKeyRepository;
	private final PortOneClient portOneClient;
	private final UserRepository userRepository;

	@Transactional
	public BillingKeyInfoResponse registerBillingKey(Long userId, BillingKeyRegisterRequest request) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

		// 1. 현재 활성 카드가 있는지 확인
		boolean hasActiveKey = billingKeyRepository.existsByUserAndIsActiveTrue(user);

		// 2. 포트원 상세 조회
		String cleanedUid = request.customerUid().trim();
		PortOneBillingKeyResponse response = portOneClient.fetchBillingKeyInfo(cleanedUid);

		// 3. 응답 검증 (포트원 응답의 status가 ISSUED가 아니면 에러 처리)
		if (response == null || !"ISSUED".equals(response.status())) {
			throw new CustomException(ErrorCode.PAYMENT_FETCH_FAILED, "유효하지 않은 빌링키 상태입니다.");
		}

		// 4. 중복 등록 확인 (마스킹된 카드번호, 카드사, PG사 조합)
		if (billingKeyRepository.existsByUserAndCardNumberAndCardNameAndPgProviderAndIsActiveTrue(
			user, response.getCardNumber(), response.getCardName(), response.getPgProvider())) {
			throw new CustomException(ErrorCode.BILLING_KEY_ALREADY_EXISTS);
		}

		// 5. DB 저장 (첫 등록이면 isDefault = true)
		UserBillingKey newBillingKey = UserBillingKey.builder()
			.user(user)
			.customerUid(response.billingKey())
			.pgProvider(response.getPgProvider())
			.cardName(response.getCardName())
			.cardNumber(response.getCardNumber())
			.isDefault(!hasActiveKey)
			.build();

		return BillingKeyInfoResponse.from(billingKeyRepository.save(newBillingKey));
	}

	@Transactional(readOnly = true)
	public List<BillingKeyInfoResponse> getMyBillingKeys(Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

		return billingKeyRepository.findAllByUserAndIsActiveTrue(user).stream()
			.sorted(Comparator.comparing(UserBillingKey::isDefault).reversed()
				.thenComparing(UserBillingKey::getCreatedAt, Comparator.reverseOrder()))
			.map(BillingKeyInfoResponse::from)
			.toList();
	}

	@Transactional
	public void changeDefaultBillingKey(Long userId, Long billingKeyId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

		// 1. 기존 대표 카드 해제
		billingKeyRepository.findByUserAndIsDefaultTrueAndIsActiveTrue(user)
			.ifPresent(key -> key.updateDefault(false));

		// 2. 새로운 대표 카드 설정
		UserBillingKey newDefaultKey = billingKeyRepository.findById(billingKeyId)
			.filter(key -> key.getUser().getId().equals(userId) && key.isActive())
			.orElseThrow(() -> new CustomException(ErrorCode.BILLING_KEY_NOT_FOUND));

		if (newDefaultKey.isDefault()) return;

		newDefaultKey.updateDefault(true);
	}

	@Transactional
	public void deleteBillingKey(Long userId, Long billingKeyId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

		UserBillingKey targetKey = billingKeyRepository.findById(billingKeyId)
			.filter(key -> key.getUser().getId().equals(userId) && key.isActive())
			.orElseThrow(() -> new CustomException(ErrorCode.BILLING_KEY_NOT_FOUND));

		boolean wasDefault = targetKey.isDefault();
		targetKey.deactivate();
		targetKey.updateDefault(false);

		// 대표 카드 삭제 시 다른 카드를 승격
		if (wasDefault) {
			billingKeyRepository.findAllByUserAndIsActiveTrue(user).stream()
				.filter(key -> !key.getId().equals(billingKeyId))
				.max(Comparator.comparing(UserBillingKey::getCreatedAt))
				.ifPresent(key -> key.updateDefault(true));
		}
	}

	@Transactional(readOnly = true)
	public String getDefaultBillingKey(Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

		return billingKeyRepository.findByUserAndIsDefaultTrueAndIsActiveTrue(user)
			.map(UserBillingKey::getCustomerUid)
			.orElseThrow(() -> new CustomException(ErrorCode.BILLING_KEY_NOT_FOUND));
	}

	@Transactional(readOnly = true)
	public Optional<BillingKeyInfoResponse> getDefaultBillingKeyInfo(Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

		return billingKeyRepository.findByUserAndIsDefaultTrueAndIsActiveTrue(user)
			.map(BillingKeyInfoResponse::from);
	}
}
