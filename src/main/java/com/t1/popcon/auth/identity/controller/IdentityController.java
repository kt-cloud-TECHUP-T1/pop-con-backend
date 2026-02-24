package com.t1.popcon.auth.identity.controller;

import com.t1.popcon.auth.identity.dto.IdentityRequest;
import com.t1.popcon.auth.identity.dto.IdentityResponse;
import com.t1.popcon.auth.identity.service.IdentityService;
import com.t1.popcon.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/identity")
@RequiredArgsConstructor
public class IdentityController {

    private final IdentityService identityService;

    @PostMapping("/complete")
    public ResponseEntity<ApiResponse<IdentityResponse.NewUserComplete>> complete(
        @Valid @RequestBody IdentityRequest.Complete request,
        @RequestHeader(value = "X-Device-Id", required = false) String deviceId
    ) {
        IdentityResponse.NewUserComplete response = identityService.complete(request, deviceId);

        return ResponseEntity.ok(
            ApiResponse.ok("본인인증이 완료되었습니다. 약관에 동의해주세요.", response)
        );
    }
}