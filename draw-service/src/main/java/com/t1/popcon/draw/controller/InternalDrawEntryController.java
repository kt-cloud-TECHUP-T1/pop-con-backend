package com.t1.popcon.draw.controller;

import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.draw.dto.response.DrawEntryResponse;
import com.t1.popcon.draw.service.DrawEntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/draws")
@RequiredArgsConstructor
public class InternalDrawEntryController {

	private final DrawEntryService drawEntryService;

	@GetMapping("/entries")
	public ApiResponse<List<DrawEntryResponse>> getEntriesByUserId(@RequestParam("userId") Long userId) {
		List<DrawEntryResponse> responses = drawEntryService.getEntriesByUserId(userId);
		return ApiResponse.ok("응모 내역 조회를 성공했습니다.", responses);
	}
}