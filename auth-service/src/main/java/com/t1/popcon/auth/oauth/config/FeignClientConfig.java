package com.t1.popcon.auth.oauth.config;

import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignClientConfig {

    @Bean
    public ErrorDecoder errorDecoder() {
        return new UserServiceFeignErrorDecoder();
    }

    static class UserServiceFeignErrorDecoder implements ErrorDecoder {
        @Override
        public Exception decode(String methodKey, Response response) {
            return new CustomException(ErrorCode.ERROR_SYSTEM, "user-service 호출에 실패했습니다.");
        }
    }
}