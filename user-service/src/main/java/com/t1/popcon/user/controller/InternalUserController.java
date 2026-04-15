package com.t1.popcon.user.controller;

import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.user.dto.PhoneUpdateRequest;
import com.t1.popcon.user.dto.UserCreateRequest;
import com.t1.popcon.user.dto.UserCreateResponse;
import com.t1.popcon.user.dto.UserInternalResponse;
import com.t1.popcon.user.dto.UserLookupResponse;
import com.t1.popcon.user.service.TestAccountGenerator;
import com.t1.popcon.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 내부 서비스 간 통신용 API
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class InternalUserController {

    private final UserService userService;
    private final TestAccountGenerator testAccountGenerator;

    /**
     * 부하 테스트용 대량 계정 생성
     * @param count 생성할 계정 수
     * @return 생성된 CSV 파일 경로
     */
    @PostMapping("/internal/test-accounts/bulk")
    public ApiResponse<String> generateTestAccounts(@RequestParam(defaultValue = "10") int count) {
        try {
            String filePath = testAccountGenerator.generateBulk(count);
            return ApiResponse.ok("성공적으로 테스트 계정이 생성되었습니다. 파일 경로: " + filePath, filePath);
        } catch (Exception e) {
            log.error("[TestAccount] 계정 생성 중 오류 발생: ", e);
            return ApiResponse.fail(ErrorCode.ERROR_SYSTEM.getCode(), "테스트 계정 생성 실패: " + e.getMessage(), null);
        }
    }

    /**
     * 생성된 테스트 계정 파일 다운로드
     * @param filePath 생성 API에서 반환된 전체 파일 경로
     * @return CSV 파일 리소스
     */
    @GetMapping("/internal/test-accounts/download")
    public ResponseEntity<Resource> downloadTestAccounts(@RequestParam String filePath) {
        try {
            // 1. 보안을 위해 허용된 베이스 디렉토리 (여기서는 /tmp로 제한)
            Path baseDir = Paths.get("/tmp").toRealPath();

            // 2. 요청된 경로 정규화 및 절대 경로 변환
            Path requestedPath = Paths.get(filePath).normalize().toAbsolutePath();

            // 3. 실제 파일의 Real Path 확인 (심볼릭 링크 등 해제)
            Path realRequestedPath = requestedPath.toRealPath();

            // 4. 베이스 디렉토리 내에 존재하는지 확인 (Path Traversal 방지)
            if (!realRequestedPath.startsWith(baseDir)) {
                log.warn("[Security] Path Traversal 시도 감지: {}", filePath);
                return ResponseEntity.badRequest().build();
            }

            File file = realRequestedPath.toFile();

            // 5. 파일 존재 여부 및 읽기 권한 확인
            if (!file.exists() || !file.isFile() || !file.canRead()) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(file);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(resource);
        } catch (IOException e) {
            log.error("[TestAccount] 파일 다운로드 중 오류 발생: ", e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 사용자 ID로 상세 정보 조회 (내부 서비스용)
     */
    @GetMapping("/internal/users/{userId}")
    public ApiResponse<UserInternalResponse> getUserInternal(@PathVariable Long userId) {
        return ApiResponse.ok(userService.getUserInternal(userId));
    }

    /**
     * 신규 회원 생성 (본인인증 완료 후 회원가입 절차)
     */
    @PostMapping("/internal/users")
    public ApiResponse<UserCreateResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        return ApiResponse.ok(userService.createUser(request));
    }

    /**
     * 소셜 로그인 정보로 사용자 조회
     */
    @GetMapping("/internal/users/social")
    public ApiResponse<UserLookupResponse> findBySocial(
            @RequestParam String provider,
            @RequestParam String providerUserId
    ) {
        return ApiResponse.ok(userService.findBySocial(provider, providerUserId));
    }

    /**
     * CI 해시로 사용자 조회 (본인인증 완료 후 기존 회원 확인)
     */
    @GetMapping("/internal/users/ci")
    public ApiResponse<UserLookupResponse> findByCiHash(
            @RequestParam String ciHash
    ) {
        return ApiResponse.ok(userService.findByCiHash(ciHash));
    }

    /**
     * CI 기반 소셜 계정 연결 (본인인증 완료 후 기존 회원이 소셜 로그인 연결)
     */
    @PostMapping("/internal/users/ci/link")
    public ApiResponse<Void> linkSocialByCi(
            @RequestParam String ciHash,
            @RequestParam String provider,
            @RequestParam String providerUserId
    ) {
        userService.linkSocialByCi(ciHash, provider, providerUserId);
        return ApiResponse.ok("소셜 계정이 연결되었습니다.");
    }

    /**
     * 휴대폰 번호 변경 (auth-service에서 본인인증 CI 검증 완료 후 호출)
     */
    @PatchMapping("/internal/users/{userId}/phone")
    public ApiResponse<Void> updatePhone(
            @PathVariable Long userId,
            @Valid @RequestBody PhoneUpdateRequest request
    ) {
        userService.updatePhone(userId, request.encryptedPhone(), request.phoneHash());
        return ApiResponse.ok("휴대폰 번호가 변경되었습니다.");
    }
}
