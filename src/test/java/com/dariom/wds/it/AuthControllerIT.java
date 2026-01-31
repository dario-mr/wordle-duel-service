package com.dariom.wds.it;

import static com.dariom.wds.it.IntegrationTestHelper.CSRF_HEADER_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.SET_COOKIE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dariom.wds.config.security.SecurityProperties;
import com.dariom.wds.persistence.repository.jpa.RefreshTokenJpaRepository;
import com.dariom.wds.service.auth.RefreshTokenService;
import com.dariom.wds.service.auth.TokenHashing;
import jakarta.annotation.Resource;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerIT {

  @Resource
  private MockMvc mockMvc;

  @Resource
  private IntegrationTestHelper itHelper;

  @Resource
  private SecurityProperties securityProperties;
  @Resource
  private RefreshTokenService refreshTokenService;
  @Resource
  private TokenHashing tokenHashing;
  @Resource
  private RefreshTokenJpaRepository refreshTokenJpaRepository;

  @Test
  void refresh_validCookieAndCsrf_returns200AndRotatesRefreshToken() throws Exception {
    // Arrange
    var user = itHelper.createUser("11111111-1111-1111-1111-111111111111", "user@test.com",
        "User");
    var oldRawToken = refreshTokenService.createRefreshToken(user);
    var oldHash = tokenHashing.sha256Hex(oldRawToken);

    var csrfCookie = itHelper.fetchCsrfCookie();
    var refreshCookie = new Cookie(securityProperties.refresh().cookieName(), oldRawToken);

    // Act
    var result = mockMvc.perform(post("/auth/refresh")
            .cookie(csrfCookie, refreshCookie)
            .header(CSRF_HEADER_NAME, csrfCookie.getValue()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isString())
        .andExpect(jsonPath("$.expiresInSeconds").value(securityProperties.jwt().ttlSeconds()))
        .andReturn();

    // Assert
    assertThat(refreshTokenJpaRepository.findByTokenHash(oldHash)).isEmpty();

    var setCookies = result.getResponse().getHeaders(SET_COOKIE);
    var newRawToken = itHelper.extractCookieValue(setCookies,
        securityProperties.refresh().cookieName());
    assertThat(newRawToken).isNotBlank();
    assertThat(newRawToken).isNotEqualTo(oldRawToken);

    var newHash = tokenHashing.sha256Hex(newRawToken);
    assertThat(refreshTokenJpaRepository.findByTokenHash(newHash)).isPresent();
  }

  @Test
  void refresh_missingRefreshCookie_returns401WithErrorCode() throws Exception {
    // Arrange
    var csrfCookie = itHelper.fetchCsrfCookie();

    // Act / Assert
    mockMvc.perform(post("/auth/refresh")
            .cookie(csrfCookie)
            .header(CSRF_HEADER_NAME, csrfCookie.getValue()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_EMPTY"))
        .andExpect(jsonPath("$.message").isString());
  }

  @Test
  void refresh_invalidRefreshCookie_returns401AndClearsRefreshCookie() throws Exception {
    // Arrange
    var csrfCookie = itHelper.fetchCsrfCookie();
    var refreshCookie = new Cookie(securityProperties.refresh().cookieName(), "not-a-valid-token");

    // Act
    var result = mockMvc.perform(post("/auth/refresh")
            .cookie(csrfCookie, refreshCookie)
            .header(CSRF_HEADER_NAME, csrfCookie.getValue()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_INVALID"))
        .andExpect(jsonPath("$.message").isString())
        .andReturn();

    // Assert
    var setCookies = result.getResponse().getHeaders(SET_COOKIE);
    var refreshSetCookieHeader = itHelper.findSetCookieHeader(setCookies,
        securityProperties.refresh().cookieName());
    assertThat(refreshSetCookieHeader).contains("Max-Age=0");
  }

  @Test
  void logout_validRefreshCookie_revokesAndClearsCookie() throws Exception {
    // Arrange
    var user = itHelper.createUser("11111111-1111-1111-1111-111111111111", "user@test.com",
        "User");
    var rawToken = refreshTokenService.createRefreshToken(user);
    var tokenHash = tokenHashing.sha256Hex(rawToken);

    var csrfCookie = itHelper.fetchCsrfCookie();
    var refreshCookie = new Cookie(securityProperties.refresh().cookieName(), rawToken);

    // Act
    var result = mockMvc.perform(post("/auth/logout")
            .cookie(csrfCookie, refreshCookie)
            .header(CSRF_HEADER_NAME, csrfCookie.getValue()))
        .andExpect(status().isNoContent())
        .andReturn();

    // Assert
    assertThat(refreshTokenJpaRepository.findByTokenHash(tokenHash)).isEmpty();

    var setCookies = result.getResponse().getHeaders(SET_COOKIE);
    var refreshSetCookieHeader = itHelper.findSetCookieHeader(setCookies,
        securityProperties.refresh().cookieName());
    assertThat(refreshSetCookieHeader).contains("Max-Age=0");
  }
}
