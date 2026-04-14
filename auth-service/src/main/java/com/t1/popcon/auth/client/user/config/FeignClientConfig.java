package com.t1.popcon.auth.client.user.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import feign.RequestInterceptor;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Slf4j
@Configuration
public class FeignClientConfig {

    @Value("${internal.api-secret}")
    private String internalSecret;

    @Bean
    public RequestInterceptor internalSecretInterceptor() {
        return requestTemplate ->
                requestTemplate.header("X-Internal-Secret", internalSecret);
    }

    @Bean
    public ErrorDecoder errorDecoder() {
        return new UserServiceFeignErrorDecoder();
    }

    static class UserServiceFeignErrorDecoder implements ErrorDecoder {

        private final ErrorDecoder defaultDecoder = new ErrorDecoder.Default();
        private final ObjectMapper objectMapper = new ObjectMapper();

        @Override
        public Exception decode(String methodKey, Response response) {
            int status = response.status();

            if (status >= 500) {
                return new CustomException(ErrorCode.ERROR_SYSTEM, "user-service 호출에 실패했습니다.");
            }
            if (status == 400) {
                return new CustomException(ErrorCode.INVALID_INPUT, "user-service 요청 값 오류");
            }
            if (status == 401) {
                return new CustomException(ErrorCode.ERROR_SYSTEM, "내부 API 인증에 실패했습니다.");
            }
            // 409 응답의 에러 코드를 파싱하여 user-service 에러를 그대로 전파
            if (status == 409) {
                return parseErrorCode(response);
            }

            return defaultDecoder.decode(methodKey, response);
        }

        /**
         * 응답 본문에서 code 필드를 파싱하여 일치하는 ErrorCode로 CustomException 반환
         * 파싱 실패 시 S001 반환
         */
        private CustomException parseErrorCode(Response response) {
            try {
                byte[] body = response.body().asInputStream().readAllBytes();
                JsonNode node = objectMapper.readTree(new String(body, StandardCharsets.UTF_8));
                String code = node.path("code").asText();

                return Arrays.stream(ErrorCode.values())
                        .filter(e -> e.getCode().equals(code))
                        .findFirst()
                        .map(CustomException::new)
                        .orElse(new CustomException(ErrorCode.ERROR_SYSTEM, "user-service 충돌 오류"));
            } catch (IOException e) {
                log.warn("[FeignClientConfig] 409 응답 파싱 실패 - method: {}", response.request().url());
                return new CustomException(ErrorCode.ERROR_SYSTEM, "user-service 충돌 오류");
            }
        }
    }
}