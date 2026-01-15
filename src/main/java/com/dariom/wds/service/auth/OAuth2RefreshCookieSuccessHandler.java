package com.dariom.wds.service.auth;

import com.dariom.wds.persistence.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

public class OAuth2RefreshCookieSuccessHandler implements AuthenticationSuccessHandler {

  private final UserRepository userRepository;
  private final RefreshTokenService refreshTokenService;
  private final RefreshTokenCookieService refreshTokenCookieService;
  private final AuthenticationSuccessHandler delegate;

  public OAuth2RefreshCookieSuccessHandler(
      UserRepository userRepository,
      RefreshTokenService refreshTokenService,
      RefreshTokenCookieService refreshTokenCookieService,
      AuthenticationSuccessHandler delegate
  ) {
    this.userRepository = userRepository;
    this.refreshTokenService = refreshTokenService;
    this.refreshTokenCookieService = refreshTokenCookieService;
    this.delegate = delegate;
  }

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request,
      HttpServletResponse response,
      Authentication authentication
  ) throws IOException, ServletException {
    var email = authentication.getName();
    var user = userRepository.requireByEmail(email);

    var refreshToken = refreshTokenService.createRefreshToken(user);
    refreshTokenCookieService.setRefreshToken(response, refreshToken);

    delegate.onAuthenticationSuccess(request, response, authentication);
  }
}
