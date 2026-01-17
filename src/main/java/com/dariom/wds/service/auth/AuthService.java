package com.dariom.wds.service.auth;

import com.dariom.wds.domain.RefreshResult;
import com.dariom.wds.exception.InvalidRefreshTokenException;
import com.dariom.wds.exception.RefreshTokenEmptyException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final RefreshTokenService refreshTokenService;
  private final RefreshTokenCookieService refreshTokenCookieService;

  public RefreshResult refreshToken(HttpServletRequest request, HttpServletResponse response) {
    var rawToken = refreshTokenCookieService.readRefreshToken(request)
        .orElseThrow(RefreshTokenEmptyException::new);

    try {
      var refreshed = refreshTokenService.refresh(rawToken);
      refreshTokenCookieService.setRefreshToken(response, refreshed.refreshToken());
      return refreshed;
    } catch (InvalidRefreshTokenException ex) {
      refreshTokenCookieService.clearRefreshToken(response);
      throw ex;
    }
  }

  public void clearRefreshToken(HttpServletRequest request, HttpServletResponse response) {
    refreshTokenCookieService.readRefreshToken(request)
        .ifPresent(refreshTokenService::revoke);
    refreshTokenCookieService.clearRefreshToken(response);
  }
}
