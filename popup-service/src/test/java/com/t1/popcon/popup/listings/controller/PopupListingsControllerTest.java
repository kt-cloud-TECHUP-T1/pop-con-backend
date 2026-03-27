package com.t1.popcon.popup.listings.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.popup.dto.card.OverlayType;
import com.t1.popcon.popup.dto.card.PhaseStatus;
import com.t1.popcon.popup.dto.card.PhaseType;
import com.t1.popcon.popup.dto.card.PopupCardDto;
import com.t1.popcon.popup.dto.section.PopupSectionResponse;
import com.t1.popcon.popup.dto.section.SectionKey;
import com.t1.popcon.popup.listings.service.PopupListingsService;
import com.t1.popcon.support.AbstractRestDocsTest;
import com.t1.popcon.support.RestDocsFactory;

import java.time.OffsetDateTime;
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
class PopupListingsControllerTest extends AbstractRestDocsTest {

    private static final String DEFAULT_URL = "/popups";
    private static final String AUCTION_URL = "/popups?phaseType=AUCTION&phaseStatus=OPEN,UPCOMING&limit=10";
    private static final String DRAW_OPEN_URL = "/popups?phaseType=DRAW&phaseStatus=OPEN&limit=10";
    private static final String DRAW_UPCOMING_URL = "/popups?phaseType=DRAW&phaseStatus=UPCOMING&sort=SOONEST_OPEN&limit=10";

    @Autowired
    private RestDocsFactory restDocsFactory;

    @MockitoBean
    private PopupListingsService popupListingsService;

    @Nested
    class 경매_드로우_목록_조회_API {

        private static final String SUMMARY = "팝업 목록 조회";
        private static final String DESCRIPTION = """
            팝업 목록을 조회합니다.
            phaseType(경매/드로우)과 phaseStatus(진행중/예정/종료)의 조합으로 원하는 팝업 목록을 조회할 수 있습니다.
            
            [현재 제공되는 섹션]
            - 경매: phaseType=AUCTION, phaseStatus=OPEN,UPCOMING
            - 드로우(진행중): phaseType=DRAW, phaseStatus=OPEN
            - 드로우(오픈예정): phaseType=DRAW, phaseStatus=UPCOMING, sort=SOONEST_OPEN
            
            ※ phaseStatus는 콤마(,)를 통해 다중 선택이 가능하여 다양한 필터 조합 조회를 지원합니다.
            
            [쿼리 파라미터]
            - phaseType: AUCTION, DRAW
            - phaseStatus: OPEN, UPCOMING, CLOSED (다중 입력 가능)
            - sort: SOONEST_OPEN (현재는 기본적으로 오픈 일시 빠른 순 정렬)
            - limit: 조회 개수 (기본 10, 1 이상)
            
            [에러 케이스]
            - 400 (C001): 잘못된 요청값
            - 500 (S001): 서버 오류
            """;

        @Test
        void 성공_경매() throws Exception {
            PopupCardDto mockCard = createMockPopupCard(
                    PhaseType.AUCTION,
                    PhaseStatus.OPEN,
                    new PopupCardDto.OverlayDto(OverlayType.AUCTION_IN_PROGRESS, null)
            );

            PopupSectionResponse<PopupCardDto> responseDto =
                new PopupSectionResponse<>(SectionKey.AUCTIONS, 0, List.of());

            ApiResponse<PopupSectionResponse<PopupCardDto>> expectedResponse =
                ApiResponse.ok("더치 경매 섹션 조회를 성공했습니다.", responseDto);

            given(popupListingsService.getPopups(any(), any(), any(), anyInt()))
                .willReturn(responseDto);

            given(popupListingsService.getMessage(any(), any()))
                .willReturn("더치 경매 섹션 조회를 성공했습니다.");

            performGet(AUCTION_URL)
                .andExpect(status().isOk())
                .andDo(
                    restDocsFactory.successWithRequestParameters(
                        "popup-listings-get-success-auction",
                        SUMMARY,
                        DESCRIPTION,
                        "Popup",
                        new PopupListingsQueryDoc("AUCTION", "OPEN,UPCOMING", null, 10),
                        null,
                        objectMapper,
                        expectedResponse
                    )
                );
        }

        @Test
        void 성공_드로우_진행중() throws Exception {
            PopupCardDto mockCard = createMockPopupCard(
                    PhaseType.DRAW,
                    PhaseStatus.OPEN,
                    null
            );

            PopupSectionResponse<PopupCardDto> responseDto =
                new PopupSectionResponse<>(SectionKey.DRAWS_OPEN, 0, List.of());

            ApiResponse<PopupSectionResponse<PopupCardDto>> expectedResponse =
                ApiResponse.ok("진행 중 드로우 섹션 조회를 성공했습니다.", responseDto);

            given(popupListingsService.getPopups(any(), any(), any(), anyInt()))
                .willReturn(responseDto);

            given(popupListingsService.getMessage(any(), any()))
                .willReturn("진행 중 드로우 섹션 조회를 성공했습니다.");

            performGet(DRAW_OPEN_URL)
                .andExpect(status().isOk())
                .andDo(
                    restDocsFactory.successWithRequestParameters(
                        "popup-listings-get-success-draw-open",
                        SUMMARY,
                        DESCRIPTION,
                        "Popup",
                        new PopupListingsQueryDoc("DRAW", "OPEN", null, 10),
                        null,
                        objectMapper,
                        expectedResponse
                    )
                );
        }

        @Test
        void 성공_드로우_오픈예정() throws Exception {
            PopupCardDto mockCard = createMockPopupCard(
                    PhaseType.DRAW,
                    PhaseStatus.UPCOMING,
                    new PopupCardDto.OverlayDto(OverlayType.DRAW_OPEN_AT, null)
            );

            PopupSectionResponse<PopupCardDto> responseDto =
                new PopupSectionResponse<>(SectionKey.DRAWS_UPCOMING, 0, List.of());

            ApiResponse<PopupSectionResponse<PopupCardDto>> expectedResponse =
                ApiResponse.ok("오픈 예정 드로우 섹션 조회를 성공했습니다.", responseDto);

            given(popupListingsService.getPopups(any(), any(), any(), anyInt()))
                .willReturn(responseDto);

            given(popupListingsService.getMessage(any(), any()))
                .willReturn("오픈 예정 드로우 섹션 조회를 성공했습니다.");

            performGet(DRAW_UPCOMING_URL)
                .andExpect(status().isOk())
                .andDo(
                    restDocsFactory.successWithRequestParameters(
                        "popup-listings-get-success-draw-upcoming",
                        SUMMARY,
                        DESCRIPTION,
                        "Popup",
                        new PopupListingsQueryDoc("DRAW", "UPCOMING", "SOONEST_OPEN", 10),
                        null,
                        objectMapper,
                        expectedResponse
                    )
                );
        }

        @Test
        void 실패_phaseType_누락() throws Exception {

            ApiResponse<Void> expectedResponse =
                ApiResponse.fail(ErrorCode.INVALID_INPUT);

            given(popupListingsService.getPopups(any(), any(), any(), anyInt()))
                .willThrow(new CustomException(ErrorCode.INVALID_INPUT));

            mockMvc.perform(
                    restDocsFactory.createRequest(
                        DEFAULT_URL + "?phaseStatus=OPEN",
                        null,
                        HttpMethod.GET,
                        objectMapper
                    )
                )
                .andExpect(status().isBadRequest())
                .andDo(
                    restDocsFactory.failure(
                        "popup-listings-get-fail-phaseType-null",
                        SUMMARY,
                        DESCRIPTION,
                        "Popup",
                        null,
                        expectedResponse
                    )
                );
        }

        @Test
        void 실패_잘못된_limit() throws Exception {
            ApiResponse<?> expectedResponse =
                invalidInput("limit", "limit는 1 이상이어야 합니다.");

            given(popupListingsService.getPopups(any(), any(), any(), anyInt()))
                .willThrow(new CustomException(
                    ErrorCode.INVALID_INPUT,
                    Map.of("limit", "limit는 1 이상이어야 합니다.")
                ));

            performGet(AUCTION_URL + "&limit=0")
                .andExpect(status().isBadRequest())
                .andDo(
                    restDocsFactory.failure(
                        "popup-listings-get-fail-invalid-limit",
                        SUMMARY,
                        DESCRIPTION,
                        "Popup",
                        null,
                        expectedResponse
                    )
                );
        }

        @Test
        void 실패_잘못된_phaseType() throws Exception {
            ApiResponse<Void> expectedResponse =
                ApiResponse.fail(ErrorCode.INVALID_INPUT);

            performGet(DEFAULT_URL + "?phaseType=INVALID&phaseStatus=OPEN")
                .andExpect(status().isBadRequest())
                .andDo(
                    restDocsFactory.failure(
                        "popup-listings-get-fail-invalid-phase-type",
                        SUMMARY,
                        DESCRIPTION,
                        "Popup",
                        null,
                        expectedResponse
                    )
                );
        }

        @Test
        void 실패_잘못된_phaseStatus() throws Exception {
            ApiResponse<Void> expectedResponse =
                ApiResponse.fail(ErrorCode.INVALID_INPUT);

            performGet(DEFAULT_URL + "?phaseType=DRAW&phaseStatus=INVALID")
                .andExpect(status().isBadRequest())
                .andDo(
                    restDocsFactory.failure(
                        "popup-listings-get-fail-invalid-phase-status",
                        SUMMARY,
                        DESCRIPTION,
                        "Popup",
                        null,
                        expectedResponse
                    )
                );
        }

        @Test
        void 실패_잘못된_sort() throws Exception {
            ApiResponse<Void> expectedResponse =
                ApiResponse.fail(ErrorCode.INVALID_INPUT);

            performGet(DEFAULT_URL + "?phaseType=DRAW&phaseStatus=UPCOMING&sort=INVALID")
                .andExpect(status().isBadRequest())
                .andDo(
                    restDocsFactory.failure(
                        "popup-listings-get-fail-invalid-sort",
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

            given(popupListingsService.getPopups(any(), any(), any(), anyInt()))
                .willThrow(new RuntimeException("Internal Server Error"));

            performGet(AUCTION_URL)
                .andExpect(status().isInternalServerError())
                .andDo(
                    restDocsFactory.failure(
                        "popup-listings-get-fail-server-error",
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

    private PopupCardDto createMockPopupCard(PhaseType phaseType, PhaseStatus phaseStatus, PopupCardDto.OverlayDto overlay) {
        return new PopupCardDto(
                1L,
                "테스트 팝업",
                null,
                "테스트 서브 텍스트",
                "테스트 캡션",
                "thumbnail_url",
                false,
                new PopupCardDto.StatsDto(20, 300),
                overlay,
                new PopupCardDto.PhaseDto(
                        phaseType,
                        phaseStatus,
                        OffsetDateTime.parse("2026-02-10T10:00:00+09:00"),
                        OffsetDateTime.parse("2026-02-11T10:00:00+09:00")
                )
        );
    }

    private ApiResponse<Void> systemError() {
        return ApiResponse.fail(ErrorCode.ERROR_SYSTEM);
    }

    private ApiResponse<?> invalidInput(String field, String message) {
        return ApiResponse.fail(ErrorCode.INVALID_INPUT, Map.of(field, message));
    }

    private record PopupListingsQueryDoc(
        String phaseType,
        String phaseStatus,
        String sort,
        Integer limit
    ) {
    }
}