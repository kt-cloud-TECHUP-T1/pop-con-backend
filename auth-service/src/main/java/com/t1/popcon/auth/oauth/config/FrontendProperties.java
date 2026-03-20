package com.t1.popcon.auth.oauth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 프론트엔드 리다이렉트 설정
 * prefix = app.frontend
 */
@ConfigurationProperties(prefix = "app.frontend")
public record FrontendProperties(
        String baseUrl,         // ex http://localhost:3000, https://dev.popcon.store
        String homePath,        // ex /
        String verifyPath,      // ex /verify
        String loginPath,       // ex /login
        String callbackPath,    // ex /auth/callback

        Boolean cookieSecure,   // ex false(local), true(dev/prod)
        String cookieSameSite,  // ex Lax(local), None(dev/prod)
        String cookieDomain     // ex dev.popcon.store
) {

    public String homeUrl() {
        return join(baseUrl, isBlank(homePath) ? "/" : homePath);
    }

    public String verifyUrl() {
        return join(baseUrl, isBlank(verifyPath) ? "/verify" : verifyPath);
    }

    public String loginUrl() {
        return join(baseUrl, isBlank(loginPath) ? "/login" : loginPath);
    }

    public String callbackUrl() {
        return join(baseUrl, isBlank(callbackPath) ? "/" : callbackPath);
    }

    public boolean isCookieSecure() {
        return Boolean.TRUE.equals(cookieSecure);
    }

    public String resolvedSameSite() {
        return isBlank(cookieSameSite) ? "Lax" : cookieSameSite;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String join(String base, String path) {
        if (base == null || base.isBlank()) return path;
        if (base.endsWith("/") && path.startsWith("/")) return base.substring(0, base.length() - 1) + path;
        if (!base.endsWith("/") && !path.startsWith("/")) return base + "/" + path;
        return base + path;
    }
}