package com.t1.popcon.auth.identity.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.t1.popcon.auth.identity.dto.PhoneChangeRequest;
import com.t1.popcon.auth.identity.dto.PhoneChangeResponse;
import com.t1.popcon.auth.identity.service.PhoneChangeService;
import com.t1.popcon.common.auth.domain.AuthUser;
import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.support.AbstractRestDocsTest;
import com.t1.popcon.support.RestDocsFactory;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class PhoneChangeControllerTest extends AbstractRestDocsTest {

    private static final String DEFAULT_URL = "/auth/identity/phone-change";
    private static final String VALID_VERIFICATION_ID = "identity-verification-0191a111-2222-7e3c-a7d4-test12345678";
    private static final Long MOCK_USER_ID = 1L;

    @Autowired
    private RestDocsFactory restDocsFactory;

    @MockitoBean
    private PhoneChangeService phoneChangeService;

    @Nested
    class 휴대폰_번호_변경_API {

        private static final String SUMMARY = "휴대폰 번호 변경";
        private static final String DESCRIPTION = """
                본인인증(PortOne) 완료 후 CI 검증을 통해 휴대폰 번호를 변경합니다.

                [헤더 파라미터]
                - Authorization: (필수) Bearer {accessToken}

                [에러 케이스]
                - 400 (C001): 본인인증 식별자 누락
                - 502 (I001): PortOne 본인인증 정보 조회 실패
                - 401 (I002): 본인 명의의 휴대폰 번호가 아닌 경우
                - 409 (U004): 이미 사용 중인 휴대폰 번호
                - 401 (A002): 유효하지 않은 accessToken
                - 401 (A003): accessToken 만료
                """;

        @Test
        void 성공() throws Exception {
            // given
            PhoneChangeRequest requestDto = new PhoneChangeRequest(VALID_VERIFICATION_ID);
            PhoneChangeResponse responseDto = new PhoneChangeResponse("010-9999-9999");
            ApiResponse<PhoneChangeResponse> expectedResponse =
                    ApiResponse.ok("휴대폰 번호가 성공적으로 변경되었습니다.", responseDto);

            given(phoneChangeService.changePhone(anyLong(), any()))
                    .willReturn(responseDto);

            // when & then
            performPost(DEFAULT_URL, requestDto)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.message").value("휴대폰 번호가 성공적으로 변경되었습니다."))
                    .andExpect(jsonPath("$.data.phone").value("010-9999-9999"))
                    .andDo(
                            restDocsFactory.success(
                                    "phone-change-success",
                                    SUMMARY,
                                    DESCRIPTION,
                                    "Auth",
                                    requestDto,
                                    expectedResponse
                            )
                    );
        }

        @Test
        void 실패_잘못된_입력값() throws Exception {
            // given
            PhoneChangeRequest requestDto = new PhoneChangeRequest("");
            ApiResponse<?> expectedResponse =
                    ApiResponse.fail(ErrorCode.INVALID_INPUT,
                            Map.of("identityVerificationId", "본인인증 식별자가 필요합니다."));

            // when & then
            performPost(DEFAULT_URL, requestDto)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("C001"))
                    .andDo(
                            restDocsFactory.failure(
                                    "phone-change-fail-invalid-input",
                                    SUMMARY,
                                    DESCRIPTION,
                                    "Auth",
                                    requestDto,
                                    expectedResponse
                            )
                    );
        }

        @Test
        void 실패_포트원_조회_실패() throws Exception {
            // given
            PhoneChangeRequest requestDto = new PhoneChangeRequest(VALID_VERIFICATION_ID);
            ApiResponse<Void> expectedResponse =
                    ApiResponse.fail(ErrorCode.IDENTITY_VERIFICATION_FETCH_FAILED);

            given(phoneChangeService.changePhone(anyLong(), any()))
                    .willThrow(new CustomException(ErrorCode.IDENTITY_VERIFICATION_FETCH_FAILED));

            // when & then
            performPost(DEFAULT_URL, requestDto)
                    .andExpect(status().isBadGateway())
                    .andExpect(jsonPath("$.code").value("I001"))
                    .andDo(
                            restDocsFactory.failure(
                                    "phone-change-fail-portone-fetch",
                                    SUMMARY,
                                    DESCRIPTION,
                                    "Auth",
                                    requestDto,
                                    expectedResponse
                            )
                    );
        }

        @Test
        void 실패_CI_불일치() throws Exception {
            // given
            PhoneChangeRequest requestDto = new PhoneChangeRequest(VALID_VERIFICATION_ID);
            ApiResponse<Void> expectedResponse =
                    ApiResponse.fail("I002", "본인 명의의 휴대폰 번호로만 변경할 수 있습니다.");

            given(phoneChangeService.changePhone(anyLong(), any()))
                    .willThrow(new CustomException(ErrorCode.IDENTITY_VERIFICATION_FAILED,
                            "본인 명의의 휴대폰 번호로만 변경할 수 있습니다."));

            // when & then
            performPost(DEFAULT_URL, requestDto)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("I002"))
                    .andExpect(jsonPath("$.message").value("본인 명의의 휴대폰 번호로만 변경할 수 있습니다."))
                    .andDo(
                            restDocsFactory.failure(
                                    "phone-change-fail-ci-mismatch",
                                    SUMMARY,
                                    DESCRIPTION,
                                    "Auth",
                                    requestDto,
                                    expectedResponse
                            )
                    );
        }

        @Test
        void 실패_이미_사용중인_번호() throws Exception {
            // given
            PhoneChangeRequest requestDto = new PhoneChangeRequest(VALID_VERIFICATION_ID);
            ApiResponse<Void> expectedResponse = ApiResponse.fail(ErrorCode.PHONE_ALREADY_IN_USE);

            given(phoneChangeService.changePhone(anyLong(), any()))
                    .willThrow(new CustomException(ErrorCode.PHONE_ALREADY_IN_USE));

            // when & then
            performPost(DEFAULT_URL, requestDto)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("U004"))
                    .andDo(
                            restDocsFactory.failure(
                                    "phone-change-fail-phone-in-use",
                                    SUMMARY,
                                    DESCRIPTION,
                                    "Auth",
                                    requestDto,
                                    expectedResponse
                            )
                    );
        }
    }

    /**
     * 인증된 사용자로 POST 요청 수행
     * authentication() 으로 SecurityContext에 AuthUser를 직접 주입
     */
    private ResultActions performPost(String url, Object body) throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                new AuthUser(MOCK_USER_ID, List.of()), null, List.of()
        );

        MockHttpServletRequestBuilder requestBuilder =
                (MockHttpServletRequestBuilder) restDocsFactory.createRequest(
                        url, body, HttpMethod.POST, objectMapper
                );

        return mockMvc.perform(requestBuilder.with(authentication(auth)));
    }
}
