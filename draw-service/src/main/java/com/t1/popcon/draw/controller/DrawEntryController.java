package com.t1.popcon.draw.controller;

import com.t1.popcon.common.auth.domain.AuthUser;
import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.common.queue.QuizPassedTokenInfo;
import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.draw.dto.request.DrawEntryRequest;
import com.t1.popcon.draw.dto.response.DrawEntryResultResponse;
import com.t1.popcon.draw.dto.response.DrawResultConfirmResponse;
import com.t1.popcon.draw.service.DrawEntryService;
import com.t1.popcon.draw.service.DrawResultService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.t1.popcon.common.queue.QuizPassedTokenFilter.QUIZ_PASSED_TOKEN_INFO_ATTRIBUTE;

@RestController
@RequestMapping("/draws")
@RequiredArgsConstructor
public class DrawEntryController {

	private final DrawEntryService drawEntryService;
  private final DrawResultService drawResultService;

	@PostMapping("/{drawId}/options/{optionId}/entries")
	public ApiResponse<DrawEntryResultResponse> applyForDraw(
		@PathVariable Long drawId,
		@PathVariable Long optionId,
		@Valid @RequestBody DrawEntryRequest request,
		@AuthenticationPrincipal AuthUser authUser,
		@RequestAttribute(QUIZ_PASSED_TOKEN_INFO_ATTRIBUTE) QuizPassedTokenInfo tokenInfo
	) {

		validateQuizToken(authUser, tokenInfo, drawId);
		DrawEntryResultResponse response = drawEntryService.applyForDraw(authUser.id(), drawId, optionId, request);


		return ApiResponse.ok("드로우 응모가 완료되었습니다.", response);
	}
  
  @PostMapping("/entries/{entryId}/confirm-result")
  public ApiResponse<DrawResultConfirmResponse> confirmResult(
      @PathVariable("entryId") Long entryId,
      @AuthenticationPrincipal AuthUser authUser
  ) {
      DrawResultConfirmResponse response = drawResultService.confirmResult(authUser.id(), entryId);
      return ApiResponse.ok("드로우 결과 확인 및 티켓 발급에 성공했습니다.", response);
  }

	private void validateQuizToken(AuthUser authUser, QuizPassedTokenInfo tokenInfo, Long drawId) {
		if (authUser == null || tokenInfo == null) {
			throw new CustomException(ErrorCode.QUIZ_PASSED_TOKEN_MISSING);
		}
		if (!authUser.id().equals(tokenInfo.userId())) {
			throw new CustomException(ErrorCode.QUIZ_PASSED_TOKEN_INVALID, "본인의 퀴즈 결과만 사용할 수 있습니다.");
		}
		if (!drawId.equals(tokenInfo.phaseId()) || !"draw".equals(tokenInfo.phaseType())) {
			throw new CustomException(ErrorCode.QUIZ_PASSED_TOKEN_INVALID, "해당 드로우에 유효한 퀴즈 토큰이 아닙니다.");
		}
	}
}
