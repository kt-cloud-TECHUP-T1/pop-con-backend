package com.t1.popcon.user.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * S3 연결 설정
 */
@Getter
@Setter
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

    /** 파일 공개 URL prefix */
    private String baseUrl;
}
