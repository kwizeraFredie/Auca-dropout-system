package rw.ac.auca.dropout.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import rw.ac.auca.dropout.service.AuthService;

import java.time.Duration;

@Service
public class AuthCookieService {
    public static final String ACCESS_COOKIE = "auca_access_token";
    public static final String REFRESH_COOKIE = "auca_refresh_token";

    private final boolean secure;

    public AuthCookieService(@Value("${security.cookies.secure}") boolean secure) {
        this.secure = secure;
    }

    public void write(HttpServletResponse response, AuthService.AuthTokens tokens) {
        response.addHeader("Set-Cookie", cookie(ACCESS_COOKIE, tokens.accessToken(), Duration.ofMinutes(15)).toString());
        response.addHeader("Set-Cookie", cookie(REFRESH_COOKIE, tokens.refreshToken(), Duration.ofDays(30)).toString());
    }

    public void clear(HttpServletResponse response) {
        response.addHeader("Set-Cookie", cookie(ACCESS_COOKIE, "", Duration.ZERO).toString());
        response.addHeader("Set-Cookie", cookie(REFRESH_COOKIE, "", Duration.ZERO).toString());
    }

    public String refreshToken(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (var cookie : request.getCookies()) {
            if (REFRESH_COOKIE.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }

    private ResponseCookie cookie(String name, String value, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}
