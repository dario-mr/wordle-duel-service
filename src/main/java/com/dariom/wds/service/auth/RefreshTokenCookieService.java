package com.dariom.wds.service.auth;

import static java.time.Duration.ZERO;
import static java.time.Duration.ofDays;
import static org.springframework.http.HttpHeaders.SET_COOKIE;

import com.dariom.wds.config.security.SecurityProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefreshTokenCookieService {

  private final SecurityProperties securityProperties;

  public Optional<String> readRefreshToken(HttpServletRequest request) {
    var cookies = request.getCookies();
    if (cookies == null || cookies.length == 0) {
      return Optional.empty();
    }

    var cookieName = securityProperties.refresh().cookieName();
    return Arrays.stream(cookies)
        .filter(c -> cookieName.equals(c.getName()))
        .findFirst()
        .map(Cookie::getValue)
        .filter(v -> !v.isBlank());
  }

  public void setRefreshToken(HttpServletResponse response, String rawToken) {
    var refreshProps = securityProperties.refresh();

    var cookie = ResponseCookie.from(refreshProps.cookieName(), rawToken)
        .httpOnly(true)
        .secure(refreshProps.cookieSecure())
        .sameSite(refreshProps.cookieSameSite())
        .path(refreshProps.cookiePath())
        .maxAge(ofDays(refreshProps.ttlDays()))
        .build();

    response.addHeader(SET_COOKIE, cookie.toString());
  }

  public void clearRefreshToken(HttpServletResponse response) {
    var refreshProps = securityProperties.refresh();

    var cookie = ResponseCookie.from(refreshProps.cookieName(), "")
        .httpOnly(true)
        .secure(refreshProps.cookieSecure())
        .sameSite(refreshProps.cookieSameSite())
        .path(refreshProps.cookiePath())
        .maxAge(ZERO)
        .build();

    response.addHeader(SET_COOKIE, cookie.toString());
  }
}
