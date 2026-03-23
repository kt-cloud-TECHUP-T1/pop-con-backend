package com.t1.popcon.auth.oauth.config;

import com.t1.popcon.auth.config.FrontendProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * OAuthProperties를 Spring Bean으로 등록
 */
@Configuration
@EnableConfigurationProperties({
        OAuthProperties.class,
        FrontendProperties.class
})

public class OAuthConfig {
}