package com.t1.popcon.auth.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PhoneChangeServiceTest {

    @Mock private PortOneClient portOneClient;
    @Mock private UserServiceClient userServiceClient;
    @Mock private EncryptionService encryptionService;

    @InjectMocks
    private PhoneChangeService phoneChangeService;

    private static final Long USER_ID = 1L;
    private static final String VERIFICATION_ID = "identity-verification-0191a111-2222-7e3c-a7d4-test12345678";
    private static final String CI_NEW = "ci-new-raw";
    private static final String CI_HASH_CURRENT = "hash-current";
    private static final String CI_HASH_NEW = "hash-new";
    private static final String PHONE_NUMBER = "01099999999";
    private static final String PHONE_HASH = "phone-hash";
    private static final String ENCRYPTED_PHONE = "encrypted-phone";

    private PortOneIdentityVerificationResponse verificationResponse;

    @BeforeEach
    void setUp() {
        PortOneIdentityVerificationResponse.VerifiedCustomer validCustomer =
                new PortOneIdentityVerificationResponse.VerifiedCustomer(
                        CI_NEW, "홍길동", "MALE", "1990-01-01", PHONE_NUMBER, false
                );
        verificationResponse = new PortOneIdentityVerificationResponse(VERIFICATION_ID, "VERIFIED", validCustomer);

        // shortHash() 내부에서 generateHash(VERIFICATION_ID) 호출 → 로그용 masking
        given(encryptionService.generateHash(VERIFICATION_ID)).willReturn("hashed-verification-id-for-log");
    }

    @Nested
    class CI_일치_성공 {

        @Test
        void CI가_일치하면_전화번호를_업데이트하고_포맷된_번호를_반환한다() {
            // given
            given(portOneClient.fetchIdentityVerification(VERIFICATION_ID)).willReturn(verificationResponse);
            given(encryptionService.generateHash(CI_NEW)).willReturn(CI_HASH_CURRENT); // 현재 사용자와 동일 CI
            given(userServiceClient.getUserInternal(USER_ID))
                    .willReturn(ApiResponse.ok(new UserInternalResponse(USER_ID, "encrypted-name", ENCRYPTED_PHONE, CI_HASH_CURRENT)));
            given(encryptionService.encrypt(PHONE_NUMBER)).willReturn(ENCRYPTED_PHONE);
            given(encryptionService.generateHash(PHONE_NUMBER)).willReturn(PHONE_HASH);
            // updatePhone 리턴값은 서비스에서 사용하지 않으므로 별도 stubbing 불필요

            // when
            PhoneChangeResponse response = phoneChangeService.changePhone(USER_ID, new PhoneChangeRequest(VERIFICATION_ID));

            // then
            assertThat(response.phone()).isEqualTo("010-9999-9999");
            // updatePhone이 올바른 encryptedPhone, phoneHash로 호출됐는지 검증
            verify(userServiceClient).updatePhone(
                    eq(USER_ID),
                    eq(new PhoneUpdateRequest(ENCRYPTED_PHONE, PHONE_HASH))
            );
        }
    }

    @Nested
    class CI_불일치_I002 {

        @Test
        void 본인_명의가_아닌_번호로_변경_시도하면_I002를_반환한다() {
            // given: 신규 CI 해시가 현재 사용자의 CI 해시와 다름
            given(portOneClient.fetchIdentityVerification(VERIFICATION_ID)).willReturn(verificationResponse);
            given(encryptionService.generateHash(CI_NEW)).willReturn(CI_HASH_NEW);
            given(userServiceClient.getUserInternal(USER_ID))
                    .willReturn(ApiResponse.ok(new UserInternalResponse(USER_ID, "encrypted-name", ENCRYPTED_PHONE, CI_HASH_CURRENT)));

            // when & then
            assertThatThrownBy(() -> phoneChangeService.changePhone(USER_ID, new PhoneChangeRequest(VERIFICATION_ID)))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode()).isEqualTo(ErrorCode.IDENTITY_VERIFICATION_FAILED));
        }
    }

    @Nested
    class 휴대폰_중복_U004 {

        @Test
        void 동일_번호가_다른_계정에_등록된_경우_U004를_반환한다() {
            // given: CI는 일치하지만 user-service에서 phoneHash 중복 감지
            given(portOneClient.fetchIdentityVerification(VERIFICATION_ID)).willReturn(verificationResponse);
            given(encryptionService.generateHash(CI_NEW)).willReturn(CI_HASH_CURRENT); // CI 일치
            given(userServiceClient.getUserInternal(USER_ID))
                    .willReturn(ApiResponse.ok(new UserInternalResponse(USER_ID, "encrypted-name", ENCRYPTED_PHONE, CI_HASH_CURRENT)));
            given(encryptionService.encrypt(PHONE_NUMBER)).willReturn(ENCRYPTED_PHONE);
            given(encryptionService.generateHash(PHONE_NUMBER)).willReturn(PHONE_HASH);
            willThrow(new CustomException(ErrorCode.PHONE_ALREADY_IN_USE))
                    .given(userServiceClient).updatePhone(eq(USER_ID), any(PhoneUpdateRequest.class));

            // when & then
            assertThatThrownBy(() -> phoneChangeService.changePhone(USER_ID, new PhoneChangeRequest(VERIFICATION_ID)))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode()).isEqualTo(ErrorCode.PHONE_ALREADY_IN_USE));
        }
    }
}
