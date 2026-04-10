package com.t1.popcon.popup.likes.controller;

import com.t1.popcon.common.auth.domain.AuthUser;
import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.popup.likes.service.PopupLikeCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/popups/{popupId}/likes")
public class PopupLikeController {

    private final PopupLikeCommandService popupLikeCommandService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> likePopup(
        @PathVariable Long popupId,
        @AuthenticationPrincipal AuthUser authUser
    ) {
        popupLikeCommandService.likePopup(popupId, authUser.id());
        return ResponseEntity.ok(ApiResponse.ok("팝업 찜 추가를 성공했습니다."));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> unlikePopup(
        @PathVariable Long popupId,
        @AuthenticationPrincipal AuthUser authUser
    ) {
        popupLikeCommandService.unlikePopup(popupId, authUser.id());
        return ResponseEntity.ok(ApiResponse.ok("팝업 찜 해제를 성공했습니다."));
    }
}
