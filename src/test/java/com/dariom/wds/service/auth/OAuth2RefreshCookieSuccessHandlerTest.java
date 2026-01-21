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
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
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

  @InjectMocks
  private OAuth2RefreshCookieSuccessHandler handler;

  @Test
  void onAuthenticationSuccess_validAuthentication_setsCookieAndDelegates() throws Exception {
    // Arrange
    var email = "user@test.com";
    var user = new AppUserEntity(UUID.randomUUID(), email, "google-sub-1", "User Test",
        "pictureUrl");
    var refreshToken = "raw-refresh-token";

    when(authentication.getName()).thenReturn(email);
    when(userRepository.requireByEmail(anyString())).thenReturn(user);
    when(refreshTokenService.createRefreshToken(any(AppUserEntity.class))).thenReturn(refreshToken);

    // Act
    handler.onAuthenticationSuccess(request, response, authentication);

    // Assert
    verify(userRepository).requireByEmail(email);
    verify(refreshTokenService).createRefreshToken(user);

    var inOrder = inOrder(refreshTokenCookieService, delegate);
    inOrder.verify(refreshTokenCookieService).setRefreshToken(response, refreshToken);
    inOrder.verify(delegate).onAuthenticationSuccess(request, response, authentication);

    verifyNoMoreInteractions(userRepository, refreshTokenService, refreshTokenCookieService,
        delegate);
  }

  @Test
  void onAuthenticationSuccess_unknownUser_throwsAndDoesNotSetCookieOrDelegate() {
    // Arrange
    var email = "missing@test.com";

    when(authentication.getName()).thenReturn(email);
    when(userRepository.requireByEmail(anyString())).thenThrow(
        new IllegalArgumentException("Unknown user"));

    // Act
    var thrown = catchThrowable(
        () -> handler.onAuthenticationSuccess(request, response, authentication));

    // Assert
    assertThat(thrown).isInstanceOf(IllegalArgumentException.class);

    verify(userRepository).requireByEmail(email);
    verifyNoMoreInteractions(userRepository);
    verifyNoInteractions(delegate, refreshTokenService, refreshTokenCookieService);
  }
}
