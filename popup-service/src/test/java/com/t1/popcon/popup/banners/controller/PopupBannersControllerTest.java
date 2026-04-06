package com.t1.popcon.popup.banners.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.popup.banners.service.PopupBannersService;
import com.t1.popcon.popup.dto.card.PopupCardDto;
import com.t1.popcon.popup.dto.section.PopupSectionResponse;
import com.t1.popcon.popup.dto.section.SectionKey;
import com.t1.popcon.support.AbstractRestDocsTest;
import com.t1.popcon.support.RestDocsFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;

@ActiveProfiles("test")
class PopupBannersControllerTest extends AbstractRestDocsTest {

    private static final String DEFAULT_URL = "/popups/banners";
    private static final String SUCCESS_URL = "/popups/banners?limit=5";

    @Autowired
    private RestDocsFactory restDocsFactory;

    @MockitoBean
    private PopupBannersService popupBannersService;

    @Nested
    class 배너_섹션_조회_API {

        private static final String SUMMARY = "배너 섹션 조회";
        private static final String DESCRIPTION = """
            홈 화면 상단 배너 섹션 데이터를 조회합니다.

            [쿼리 파라미터]
            - limit: 조회할 배너 개수 (1~5)

            [에러 케이스]
            - 400 (C001): 잘못된 요청값
            - 500 (S001): 서버 오류
            """;

        @Test
        void 성공() throws Exception {
            PopupSectionResponse<PopupCardDto> responseDto =
                new PopupSectionResponse<>(SectionKey.BANNERS, 0, List.of());

            ApiResponse<PopupSectionResponse<PopupCardDto>> expectedResponse =
                ApiResponse.ok("배너 섹션 조회를 성공했습니다.", responseDto);

            given(popupBannersService.getBanners(anyInt()))
                .willReturn(responseDto);

            performGet(SUCCESS_URL)
                .andExpect(status().isOk())
                .andDo(
                    restDocsFactory.successWithRequestParameters(
                        "popup-banners-get-success",
                        SUMMARY,
                        DESCRIPTION,
                        "Popup",
                        new PopupBannersQueryDoc(5),
                        null,
                        objectMapper,
                        expectedResponse
                    )
                );
        }

        @Test
        void 실패_잘못된_limit() throws Exception {
            ApiResponse<?> expectedResponse =
                invalidInput("limit", "limit는 1 이상 5 이하여야 합니다.");

            given(popupBannersService.getBanners(anyInt()))
                .willThrow(new CustomException(
                    ErrorCode.INVALID_INPUT,
                    Map.of("limit", "limit는 1 이상 5 이하여야 합니다.")
                ));

            performGet(DEFAULT_URL + "?limit=0")
                .andExpect(status().isBadRequest())
                .andDo(
                    restDocsFactory.failure(
                        "popup-banners-get-fail-invalid-limit",
                        SUMMARY,
                        DESCRIPTION,
                        "Popup",
                        null,
                        expectedResponse
                    )
                );
        }

        @Test
        void 실패_서버_오류() throws Exception {
            ApiResponse<Void> expectedResponse = systemError();

            given(popupBannersService.getBanners(anyInt()))
                .willThrow(new RuntimeException("Internal Server Error"));

            performGet(DEFAULT_URL)
                .andExpect(status().isInternalServerError())
                .andDo(
                    restDocsFactory.failure(
                        "popup-banners-get-fail-server-error",
                        SUMMARY,
                        DESCRIPTION,
                        "Popup",
                        null,
                        expectedResponse
                    )
                );
        }
    }

    private ResultActions performGet(String url) throws Exception {
        return mockMvc.perform(
            restDocsFactory.createRequest(
                url,
                null,
                HttpMethod.GET,
                objectMapper
            )
        );
    }

    private ApiResponse<Void> systemError() {
        return ApiResponse.fail(ErrorCode.ERROR_SYSTEM);
    }

    private ApiResponse<?> invalidInput(String field, String message) {
        return ApiResponse.fail(ErrorCode.INVALID_INPUT, Map.of(field, message));
    }

    private record PopupBannersQueryDoc(
        Integer limit
    ) {
    }
}