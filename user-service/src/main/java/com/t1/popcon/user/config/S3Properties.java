package com.t1.popcon.user.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * S3 연결 설정
 * baseUrl은 trailing slash 없이 설정 (setBaseUrl에서 자동 제거)
 * ex) https://assets.popcon.store (O) / https://assets.popcon.store/ (X)
 */
@Getter
@Component
@ConfigurationProperties(prefix = "s3")
public class S3Properties {

    /** S3 엔드포인트 URL */
    private String endpoint;

    /** 버킷 이름 */
    private String bucket;

    /** 리전 */
    private String region;

    /** 액세스 키 */
    private String accessKey;

    /** 시크릿 키 */
    private String secretKey;

    /**
     * 파일 공개 URL prefix
     * trailing slash 자동 제거 - S3Service.extractKey()와 URL 조합 시 일관성 보장
     */
    private String baseUrl;

    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public void setBucket(String bucket) { this.bucket = bucket; }
    public void setRegion(String region) { this.region = region; }
    public void setAccessKey(String accessKey) { this.accessKey = accessKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = (baseUrl != null && baseUrl.endsWith("/"))
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;
    }
}
