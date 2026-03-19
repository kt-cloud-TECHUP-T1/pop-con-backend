package com.t1.popcon.auth.oauth.config;

import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import feign.RequestInterceptor;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignClientConfig {

    @Value("${services.user-service.internal-secret}")
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

            return defaultDecoder.decode(methodKey, response);
        }
    }
}