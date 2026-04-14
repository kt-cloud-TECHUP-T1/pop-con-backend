package com.t1.popcon.auth.identity.service;

import com.t1.popcon.auth.client.user.UserServiceClient;
import com.t1.popcon.auth.client.user.dto.PhoneUpdateRequest;
import com.t1.popcon.auth.client.user.dto.UserInternalResponse;
import com.t1.popcon.auth.identity.dto.PhoneChangeRequest;
import com.t1.popcon.auth.identity.dto.PhoneChangeResponse;
import com.t1.popcon.common.encryption.EncryptionService;
import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.common.infrastructure.dto.PortOneIdentityVerificationResponse;
import com.t1.popcon.common.infrastructure.portone.PortOneClient;
import com.t1.popcon.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PhoneChangeService {

    private final PortOneClient portOneClient;
    private final UserServiceClient userServiceClient;
    private final EncryptionService encryptionService;

    /**
     * 휴대폰 번호 변경
     * 1. PortOne 본인인증 결과 조회 및 검증
     * 2. 신규 CI 해시와 현재 사용자의 CI 해시 비교
     * 3. 일치 시 암호화된 번호로 업데이트
     */
    public PhoneChangeResponse changePhone(Long userId, PhoneChangeRequest request) {
        log.info("휴대폰 번호 변경 시작 - identityVerificationIdHash: {}, userId: {}",
                shortHash(request.identityVerificationId()), userId % 1000);

        // PortOne 본인인증 결과 조회
        PortOneIdentityVerificationResponse verification =
                portOneClient.fetchIdentityVerification(request.identityVerificationId());

        if (!verification.isVerified() || verification.verifiedCustomer() == null) {
            log.warn("본인인증 검증 실패 - status: {}", verification.status());
            throw new CustomException(ErrorCode.IDENTITY_VERIFICATION_FAILED);
        }

        PortOneIdentityVerificationResponse.VerifiedCustomer customer = verification.verifiedCustomer();
        validateVerifiedCustomer(customer);

        // 신규 CI 해시 생성
        String ciHashNew = encryptionService.generateHash(customer.ci());

        // 현재 사용자의 CI 해시 조회
        ApiResponse<UserInternalResponse> userResponse = userServiceClient.getUserInternal(userId);
        if (userResponse.getData() == null) {
            log.warn("사용자 정보 조회 실패 - userId: {}", userId % 1000);
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }
        String ciHashCurrent = userResponse.getData().ciHash();

        // CI 해시 비교 (다른 명의 휴대폰 사용 방지)
        if (!ciHashNew.equals(ciHashCurrent)) {
            throw new CustomException(ErrorCode.IDENTITY_VERIFICATION_FAILED, "본인 명의의 휴대폰 번호로만 변경할 수 있습니다.");
        }

        // 새 휴대폰 번호 암호화 및 해시 생성 후 저장
        // phoneHash로 타 계정 중복 여부는 user-service에서 검증
        String encryptedPhone = encryptionService.encrypt(customer.phoneNumber());
        String phoneHash = encryptionService.generateHash(customer.phoneNumber());
        userServiceClient.updatePhone(userId, new PhoneUpdateRequest(encryptedPhone, phoneHash));

        log.info("휴대폰 번호 변경 완료 - userId: {}", userId % 1000);
        return new PhoneChangeResponse(formatPhone(customer.phoneNumber()));
    }

    /**
     * CI(해시 비교용)와 phoneNumber(저장용) 검증
     */
    private void validateVerifiedCustomer(PortOneIdentityVerificationResponse.VerifiedCustomer customer) {
        if (customer.ci() == null || customer.ci().isBlank()) {
            throw new CustomException(ErrorCode.IDENTITY_VERIFICATION_FAILED);
        }
        if (customer.phoneNumber() == null || customer.phoneNumber().isBlank()) {
            throw new CustomException(ErrorCode.IDENTITY_VERIFICATION_FAILED);
        }
    }

    /** 전화번호를 010-XXXX-XXXX 형식으로 변환 */
    private String formatPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return phone;
        }
        String trimmed = phone.trim();
        if (trimmed.contains("-")) {
            return trimmed;
        }
        // 11자리 순수 숫자인 경우만 하이픈 포맷 적용
        if (trimmed.length() == 11 && trimmed.matches("\\d+")) {
            return trimmed.substring(0, 3) + "-" + trimmed.substring(3, 7) + "-" + trimmed.substring(7);
        }
        return trimmed;
    }

    /**
     * 로그용 단방향 해시 앞 8자리만 반환 (민감정보 마스킹)
     */
    private String shortHash(String value) {
        if (value == null || value.isBlank()) {
            return "null";
        }
        String hashed = encryptionService.generateHash(value);
        return hashed.length() <= 8 ? hashed : hashed.substring(0, 8);
    }
}
