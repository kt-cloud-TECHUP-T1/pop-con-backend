package com.t1.popcon.user.service;

import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.user.config.S3Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

/**
 * S3(KT Cloud Object Storage) 파일 업로드/삭제 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024L; // 5MB
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png");
    private static final String PROFILE_KEY_PREFIX = "profiles/";

    private final S3Client s3Client;
    private final S3Properties s3Properties;

    /**
     * 프로필 이미지 업로드
     * - 파일 형식: JPG, PNG만 허용
     * - 파일 크기: 5MB 이하
     *
     * @return 업로드된 파일의 공개 URL
     */
    public String uploadProfileImage(Long userId, MultipartFile file) {
        validateFile(file);

        String key = buildKey(userId, file.getOriginalFilename());

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(s3Properties.getBucket())
                            .key(key)
                            .contentType(file.getContentType())
                            .contentLength(file.getSize())
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
        } catch (IOException e) {
            log.error("[S3] 프로필 이미지 업로드 실패 - userId: {}, key: {}", userId, key, e);
            throw new CustomException(ErrorCode.ERROR_SYSTEM);
        }

        String url = s3Properties.getBaseUrl() + "/" + key;
        log.info("[S3] 프로필 이미지 업로드 완료 - userId: {}, url: {}", userId, url);
        return url;
    }

    /**
     * 프로필 이미지 삭제
     * URL에서 S3 key를 추출하여 삭제 요청
     */
    public void deleteProfileImage(String imageUrl) {
        String key = extractKey(imageUrl);

        s3Client.deleteObject(
                DeleteObjectRequest.builder()
                        .bucket(s3Properties.getBucket())
                        .key(key)
                        .build()
        );

        log.info("[S3] 프로필 이미지 삭제 완료 - key: {}", key);
    }

    /** 파일 형식 및 크기 검증 */
    private void validateFile(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new CustomException(ErrorCode.INVALID_FILE_TYPE);
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new CustomException(ErrorCode.FILE_SIZE_EXCEEDED);
        }
    }

    /** S3 오브젝트 키 생성: profiles/{userId}/{uuid}.{ext} */
    private String buildKey(Long userId, String originalFilename) {
        String ext = extractExtension(originalFilename);
        return PROFILE_KEY_PREFIX + userId + "/" + UUID.randomUUID() + "." + ext;
    }

    /** URL에서 S3 key 추출 */
    private String extractKey(String imageUrl) {
        String baseUrl = s3Properties.getBaseUrl();
        if (imageUrl.startsWith(baseUrl)) {
            return imageUrl.substring(baseUrl.length() + 1); // '/' 포함 제거
        }
        // base-url 매핑 실패 시 원문 반환 (삭제 시도)
        return imageUrl;
    }

    /** 파일명에서 확장자 추출 */
    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "jpg";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
