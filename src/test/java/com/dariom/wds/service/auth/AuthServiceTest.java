package com.dariom.wds.service.auth;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.dariom.wds.domain.AccessToken;
import com.dariom.wds.domain.RefreshResult;
import com.dariom.wds.exception.InvalidRefreshTokenException;
import com.dariom.wds.exception.RefreshTokenEmptyException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock
  private RefreshTokenService refreshTokenService;
  @Mock
  private RefreshTokenCookieService refreshTokenCookieService;
  @Mock
  private HttpServletRequest request;
  @Mock
  private HttpServletResponse response;

  @InjectMocks
  private AuthService service;

  @Test
  void refreshToken_cookieMissing_throwsAndDoesNotTouchResponse() {
    // Arrange
    when(refreshTokenCookieService.readRefreshToken(any(HttpServletRequest.class))).thenReturn(
        empty());

    // Act
    var thrown = catchThrowable(() -> service.refreshToken(request, response));

    // Assert
    assertThat(thrown).isInstanceOf(RefreshTokenEmptyException.class);

    verify(refreshTokenCookieService).readRefreshToken(request);
    verifyNoMoreInteractions(refreshTokenCookieService);
    verifyNoInteractions(refreshTokenService);
  }

  @Test
  void refreshToken_validToken_refreshesAndSetsCookie() {
    // Arrange
    var rawToken = "raw-token";
    var refreshed = new RefreshResult("new-refresh", new AccessToken("access", 900));

    when(refreshTokenCookieService.readRefreshToken(any(HttpServletRequest.class))).thenReturn(
        of(rawToken));
    when(refreshTokenService.refresh(anyString())).thenReturn(refreshed);

    // Act
    var result = service.refreshToken(request, response);

    // Assert
    assertThat(result).isEqualTo(refreshed);

    verify(refreshTokenCookieService).readRefreshToken(request);
    verify(refreshTokenService).refresh(rawToken);
    verify(refreshTokenCookieService).setRefreshToken(response, "new-refresh");
    verifyNoMoreInteractions(refreshTokenService, refreshTokenCookieService);
  }

  @Test
  void refreshToken_invalidToken_clearsCookieAndRethrows() {
    // Arrange
    var rawToken = "raw-token";

    when(refreshTokenCookieService.readRefreshToken(any(HttpServletRequest.class))).thenReturn(
        of(rawToken));
    when(refreshTokenService.refresh(anyString())).thenThrow(new InvalidRefreshTokenException());

    // Act
    var thrown = catchThrowable(() -> service.refreshToken(request, response));

    // Assert
    assertThat(thrown).isInstanceOf(InvalidRefreshTokenException.class);

    verify(refreshTokenCookieService).readRefreshToken(request);
    verify(refreshTokenService).refresh(rawToken);
    verify(refreshTokenCookieService).clearRefreshToken(response);
    verifyNoMoreInteractions(refreshTokenService, refreshTokenCookieService);
  }

  @Test
  void clearRefreshToken_cookieExists_revokesAndClears() {
    // Arrange
    var rawToken = "raw-token";
    when(refreshTokenCookieService.readRefreshToken(any(HttpServletRequest.class))).thenReturn(
        of(rawToken));

    // Act
    service.clearRefreshToken(request, response);

    // Assert
    verify(refreshTokenCookieService).readRefreshToken(request);
    verify(refreshTokenService).revoke(rawToken);
    verify(refreshTokenCookieService).clearRefreshToken(response);
    verifyNoMoreInteractions(refreshTokenService, refreshTokenCookieService);
  }

  @Test
  void clearRefreshToken_cookieMissing_onlyClears() {
    // Arrange
    when(refreshTokenCookieService.readRefreshToken(any(HttpServletRequest.class))).thenReturn(
        empty());

    // Act
    service.clearRefreshToken(request, response);

    // Assert
    verify(refreshTokenCookieService).readRefreshToken(request);
    verify(refreshTokenCookieService).clearRefreshToken(response);
    verifyNoMoreInteractions(refreshTokenCookieService);
    verifyNoInteractions(refreshTokenService);
  }
}
