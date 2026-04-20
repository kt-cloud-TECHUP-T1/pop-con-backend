package com.t1.popcon.user.controller;

import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.user.service.TestAccountGenerator;
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
 * 부하 테스트 및 개발 지원용 API
 */
@Slf4j
@RestController
@RequestMapping("/users/internal/test-accounts")
@RequiredArgsConstructor
public class TestAccountController {

    private final TestAccountGenerator testAccountGenerator;

    /**
     * 부하 테스트용 대량 계정 생성
     * @param count 생성할 계정 수
     * @return 생성된 CSV 파일 경로
     */
    @PostMapping("/bulk")
    public ApiResponse<String> generateTestAccounts(@RequestParam(defaultValue = "10") int count) {
        if (count < 1 || count > 50000) {
            return ApiResponse.fail(ErrorCode.ERROR_SYSTEM.getCode(), "생성할 계정 수는 1개에서 50,000개 사이여야 합니다. (요청: " + count + ")", null);
        }

        try {
            String filePath = testAccountGenerator.generateBulk(count);
            return ApiResponse.ok("성공적으로 테스트 계정이 생성되었습니다. 파일 경로: " + filePath, filePath);
        } catch (Exception e) {
            log.error("[TestAccount] 계정 생성 중 오류 발생: ", e);
            return ApiResponse.fail(ErrorCode.ERROR_SYSTEM.getCode(), "테스트 계정 생성 실패", null);
        }
    }

    /**
     * 시연용 슈퍼 계정 생성
     * @param count 생성할 슈퍼 계정 수
     */
    @PostMapping("/super")
    public ApiResponse<Void> generateSuperAccounts(@RequestParam(defaultValue = "100") int count) {
        if (count < 1 || count > 1000) {
            return ApiResponse.fail(ErrorCode.ERROR_SYSTEM.getCode(), "생성할 슈퍼 계정 수는 1개에서 1,000개 사이여야 합니다. (요청: " + count + ")", null);
        }

        try {
            testAccountGenerator.generateSuperAccounts(count);
            return ApiResponse.ok("성공적으로 슈퍼 계정이 생성되었습니다.", null);
        } catch (Exception e) {
            log.error("[SuperAccount] 슈퍼 계정 생성 중 오류 발생: ", e);
            return ApiResponse.fail(ErrorCode.ERROR_SYSTEM.getCode(), "슈퍼 계정 생성 실패", null);
        }
    }

    /**
     * 생성된 테스트 계정 파일 다운로드
     * @param filePath 생성 API에서 반환된 전체 파일 경로
     * @return CSV 파일 리소스
     */
    @GetMapping("/download")
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

            // 5. 파일명 패턴 검증 (프로젝트 패턴: load_test_accounts_*.csv)
            String fileName = realRequestedPath.getFileName().toString();
            if (!fileName.matches("^load_test_accounts_.*\\.csv$")) {
                log.warn("[Security] 허용되지 않은 파일 형식 다운로드 시도: {}", fileName);
                return ResponseEntity.status(403).build();
            }

            File file = realRequestedPath.toFile();

            // 6. 파일 존재 여부 및 읽기 권한 확인
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
}
