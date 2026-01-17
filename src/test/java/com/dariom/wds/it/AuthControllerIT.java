package com.dariom.wds.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.SET_COOKIE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dariom.wds.config.security.SecurityProperties;
import com.dariom.wds.persistence.entity.AppUserEntity;
import com.dariom.wds.persistence.entity.RoleEntity;
import com.dariom.wds.persistence.repository.jpa.AppUserJpaRepository;
import com.dariom.wds.persistence.repository.jpa.RefreshTokenJpaRepository;
import com.dariom.wds.persistence.repository.jpa.RoleJpaRepository;
import com.dariom.wds.service.auth.RefreshTokenService;
import com.dariom.wds.service.auth.TokenHashing;
import jakarta.annotation.Resource;
import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerIT {

  private static final String CSRF_COOKIE_NAME = "WD-XSRF-TOKEN";
  private static final String CSRF_HEADER_NAME = "X-WD-XSRF-TOKEN";

  @Resource
  private MockMvc mockMvc;
  @Resource
  private SecurityProperties securityProperties;
  @Resource
  private AppUserJpaRepository appUserJpaRepository;
  @Resource
  private RoleJpaRepository roleJpaRepository;
  @Resource
  private RefreshTokenService refreshTokenService;
  @Resource
  private TokenHashing tokenHashing;
  @Resource
  private RefreshTokenJpaRepository refreshTokenJpaRepository;

  @Test
  void refresh_validCookieAndCsrf_returns200AndRotatesRefreshToken() throws Exception {
    // Arrange
    var user = createUserWithRole("user@test.com", "USER");
    var oldRawToken = refreshTokenService.createRefreshToken(user);
    var oldHash = tokenHashing.sha256Hex(oldRawToken);

    var csrfCookie = fetchCsrfCookie();
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
    var newRawToken = extractCookieValue(setCookies, securityProperties.refresh().cookieName());
    assertThat(newRawToken).isNotBlank();
    assertThat(newRawToken).isNotEqualTo(oldRawToken);

    var newHash = tokenHashing.sha256Hex(newRawToken);
    assertThat(refreshTokenJpaRepository.findByTokenHash(newHash)).isPresent();
  }

  @Test
  void refresh_missingRefreshCookie_returns401WithErrorCode() throws Exception {
    // Arrange
    var csrfCookie = fetchCsrfCookie();

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
    var csrfCookie = fetchCsrfCookie();
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
    var refreshSetCookieHeader = findSetCookieHeader(setCookies,
        securityProperties.refresh().cookieName());
    assertThat(refreshSetCookieHeader).contains("Max-Age=0");
  }

  @Test
  void logout_validRefreshCookie_revokesAndClearsCookie() throws Exception {
    // Arrange
    var user = createUserWithRole("user@test.com", "USER");
    var rawToken = refreshTokenService.createRefreshToken(user);
    var tokenHash = tokenHashing.sha256Hex(rawToken);

    var csrfCookie = fetchCsrfCookie();
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
    var refreshSetCookieHeader = findSetCookieHeader(setCookies,
        securityProperties.refresh().cookieName());
    assertThat(refreshSetCookieHeader).contains("Max-Age=0");
  }

  private Cookie fetchCsrfCookie() throws Exception {
    var response = mockMvc.perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse();

    var csrfCookie = response.getCookie(CSRF_COOKIE_NAME);
    assertThat(csrfCookie).isNotNull();
    assertThat(csrfCookie.getValue()).isNotBlank();

    return csrfCookie;
  }

  private AppUserEntity createUserWithRole(String email, String roleName) {
    var role = roleJpaRepository.findById(roleName)
        .orElseGet(() -> roleJpaRepository.save(new RoleEntity(roleName)));

    var user = new AppUserEntity(UUID.randomUUID(), email, "google-sub-1", "User Test");
    user.addRole(role);

    return appUserJpaRepository.save(user);
  }

  private static String extractCookieValue(List<String> setCookies, String cookieName) {
    var header = findSetCookieHeader(setCookies, cookieName);

    var start = cookieName.length() + 1;
    var end = header.indexOf(';');
    if (end == -1) {
      end = header.length();
    }

    return header.substring(start, end);
  }

  private static String findSetCookieHeader(List<String> setCookies, String cookieName) {
    return setCookies.stream()
        .filter(h -> h.startsWith(cookieName + "="))
        .findFirst()
        .orElseThrow(
            () -> new AssertionError("Missing Set-Cookie for " + cookieName + ": " + setCookies));
  }
}
