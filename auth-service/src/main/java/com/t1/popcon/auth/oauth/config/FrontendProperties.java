package com.t1.popcon.auth.oauth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 프론트엔드 리다이렉트 설정
 * prefix = app.frontend
 */
@ConfigurationProperties(prefix = "app.frontend")
public record FrontendProperties(
        String baseUrl,     // ex) http://localhost:3000, https://dev.popcon.store
        String homePath,    // ex) /
        String verifyPath,  // ex) /verify
        String loginPath    // ex) /login
) {

    public String homeUrl() {
        return join(baseUrl, (homePath == null || homePath.isBlank()) ? "/" : homePath);
    }

    public String verifyUrl() {
        return join(baseUrl, (verifyPath == null || verifyPath.isBlank()) ? "/verify" : verifyPath);
    }

    public String loginUrl() {
        return join(baseUrl, (loginPath == null || loginPath.isBlank()) ? "/login" : loginPath);
    }

    private static String join(String base, String path) {
        if (base == null || base.isBlank()) return path;
        if (base.endsWith("/") && path.startsWith("/")) return base.substring(0, base.length() - 1) + path;
        if (!base.endsWith("/") && !path.startsWith("/")) return base + "/" + path;
        return base + path;
    }
}