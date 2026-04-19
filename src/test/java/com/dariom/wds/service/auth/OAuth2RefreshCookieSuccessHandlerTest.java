package com.dariom.wds.service.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.dariom.wds.persistence.entity.AppUserEntity;
import com.dariom.wds.persistence.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@ExtendWith(MockitoExtension.class)
class OAuth2RefreshCookieSuccessHandlerTest {

  @Mock
  private UserRepository userRepository;
  @Mock
  private RefreshTokenService refreshTokenService;
  @Mock
  private RefreshTokenCookieService refreshTokenCookieService;
  @Mock
  private AuthenticationSuccessHandler delegate;
  @Mock
  private HttpServletRequest request;
  @Mock
  private HttpServletResponse response;
  @Mock
  private Authentication authentication;
  @Mock
  private OidcUser oidcUser;

  @InjectMocks
  private OAuth2RefreshCookieSuccessHandler handler;

  @Test
  void onAuthenticationSuccess_validAuthentication_setsCookieAndDelegates() throws Exception {
    // Arrange
    var userId = UUID.randomUUID().toString();
    var user = new AppUserEntity(UUID.fromString(userId), "user@test.com", "google-sub-1", "User Test",
        "pictureUrl");
    var refreshToken = "raw-refresh-token";

    when(authentication.getPrincipal()).thenReturn(oidcUser);
    when(oidcUser.getAttribute(OAuthUserService.APP_USER_ID_CLAIM)).thenReturn(userId);
    when(userRepository.findById(anyString())).thenReturn(Optional.of(user));
    when(refreshTokenService.createRefreshToken(any(AppUserEntity.class))).thenReturn(refreshToken);

    // Act
    handler.onAuthenticationSuccess(request, response, authentication);

    // Assert
    verify(userRepository).findById(userId);
    verify(refreshTokenService).createRefreshToken(user);

    var inOrder = inOrder(refreshTokenCookieService, delegate);
    inOrder.verify(refreshTokenCookieService).setRefreshToken(response, refreshToken);
    inOrder.verify(delegate).onAuthenticationSuccess(request, response, authentication);

    verifyNoMoreInteractions(userRepository, refreshTokenService, refreshTokenCookieService,
        delegate);
  }

  @Test
  void onAuthenticationSuccess_missingAppUserId_throwsAndDoesNotSetCookieOrDelegate() {
    // Arrange
    when(authentication.getPrincipal()).thenReturn(oidcUser);
    when(oidcUser.getAttribute(OAuthUserService.APP_USER_ID_CLAIM)).thenReturn(null);

    // Act
    var thrown = catchThrowable(
        () -> handler.onAuthenticationSuccess(request, response, authentication));

    // Assert
    assertThat(thrown)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("OIDC principal is missing local app user id");

    verifyNoInteractions(userRepository);
    verifyNoInteractions(delegate, refreshTokenService, refreshTokenCookieService);
  }

  @Test
  void onAuthenticationSuccess_unknownUser_throwsAndDoesNotSetCookieOrDelegate() {
    // Arrange
    var userId = UUID.randomUUID().toString();

    when(authentication.getPrincipal()).thenReturn(oidcUser);
    when(oidcUser.getAttribute(OAuthUserService.APP_USER_ID_CLAIM)).thenReturn(userId);
    when(userRepository.findById(anyString())).thenReturn(Optional.empty());

    // Act
    var thrown = catchThrowable(
        () -> handler.onAuthenticationSuccess(request, response, authentication));

    // Assert
    assertThat(thrown)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Unknown user: " + userId);

    verify(userRepository).findById(userId);
    verifyNoMoreInteractions(userRepository);
    verifyNoInteractions(delegate, refreshTokenService, refreshTokenCookieService);
  }
}
