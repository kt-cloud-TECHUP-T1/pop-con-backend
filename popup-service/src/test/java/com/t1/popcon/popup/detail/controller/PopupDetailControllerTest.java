package com.t1.popcon.popup.detail.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.popup.detail.dto.PopupDetailResponse;
import com.t1.popcon.popup.detail.service.PopupDetailService;
import com.t1.popcon.support.AbstractRestDocsTest;
import com.t1.popcon.support.RestDocsFactory;

@ActiveProfiles("test")
class PopupDetailControllerTest extends AbstractRestDocsTest {

	private static final String DEFAULT_URL = "/popups";

	@Autowired
	private RestDocsFactory restDocsFactory;

	@MockitoBean
	private PopupDetailService popupDetailService;

	@Nested
	class 팝업스토어_상세_조회_API {

		private static final String SUMMARY = "팝업스토어 상세 조회";
		private static final String DESCRIPTION = """
			팝업스토어의 상세 정보를 조회합니다.
			로그인 여부에 따라 좋아요(liked) 상태가 포함됩니다.
			
			[에러 케이스]
			- 500 (S001): 서버 오류
			""";

		@Test
		void 성공() throws Exception {
			// given
			Long popupId = 1L;
			PopupDetailResponse responseDto = PopupDetailResponse.ofMock(popupId);
			ApiResponse<PopupDetailResponse> expectedResponse = ApiResponse.ok("팝업스토어 조회를 성공했습니다.", responseDto);

			given(popupDetailService.getPopupDetail(anyLong())).willReturn(responseDto);

			// when & then
			mockMvc.perform(
					restDocsFactory.createRequest(
						DEFAULT_URL + "/{popupId}",
						null,
						HttpMethod.GET,
						objectMapper,
						popupId
					)
				)
				.andExpect(status().isOk())
				.andDo(
					restDocsFactory.success(
						"popup-detail-get-success",
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
			// given
			Long popupId = 1L;
			ApiResponse<Void> expectedResponse = ApiResponse.fail(ErrorCode.ERROR_SYSTEM);

			given(popupDetailService.getPopupDetail(anyLong()))
				.willThrow(new RuntimeException("Internal Server Error"));

			// when & then
			mockMvc.perform(
					restDocsFactory.createRequest(
						DEFAULT_URL + "/{popupId}",
						null,
						HttpMethod.GET,
						objectMapper,
						popupId
					)
				)
				.andExpect(status().isInternalServerError())
				.andDo(
					restDocsFactory.failure(
						"popup-detail-get-fail-server-error",
						SUMMARY,
						DESCRIPTION,
						"Popup",
						null,
						expectedResponse
					)
				);
		}
	}
}
