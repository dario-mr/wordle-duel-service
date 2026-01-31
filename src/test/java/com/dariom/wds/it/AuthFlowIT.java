package com.dariom.wds.it;

import static com.dariom.wds.it.IntegrationTestHelper.CSRF_HEADER_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.SET_COOKIE;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dariom.wds.config.security.SecurityProperties;
import com.dariom.wds.persistence.entity.RefreshTokenEntity;
import com.dariom.wds.persistence.repository.jpa.RefreshTokenJpaRepository;
import com.dariom.wds.service.auth.RefreshTokenService;
import com.dariom.wds.service.auth.TokenHashing;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthFlowIT {

  private static final String ROOMS_URL = "/api/v1/rooms";

  @Resource
  private ObjectMapper objectMapper;

  @Resource
  private SecurityProperties securityProperties;

  @Resource
  private MockMvc mockMvc;

  @Resource
  private IntegrationTestHelper itHelper;

  @Resource
  private RefreshTokenService refreshTokenService;

  @Resource
  private TokenHashing tokenHashing;

  @Resource
  private RefreshTokenJpaRepository refreshTokenJpaRepository;

  @Test
  void authFlow_refreshAllowsApiCall_logoutRevokesAndPreventsRefresh() throws Exception {
    // Creating a room without a Bearer token returns 401
    mockMvc.perform(post(ROOMS_URL)
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("language", "IT"))))
        .andExpect(status().isUnauthorized());

    // Create a real user + role and seed a refresh token directly into the DB
    var user = itHelper.createUser("11111111-1111-1111-1111-111111111111", "user@test.com",
        "User");
    var oldRawRefreshToken = refreshTokenService.createRefreshToken(user);

    // Fetch a CSRF cookie (XSRF-TOKEN) so we can call the auth endpoints
    var csrfCookie = itHelper.fetchCsrfCookie();
    var csrfToken = csrfCookie.getValue();
    var refreshCookieName = securityProperties.refresh().cookieName();

    // Exchange refresh cookie (+ CSRF header) for a short-lived access token
    var refreshRes = mockMvc.perform(post("/auth/refresh")
            .cookie(csrfCookie)
            .cookie(new jakarta.servlet.http.Cookie(refreshCookieName, oldRawRefreshToken))
            .header(CSRF_HEADER_NAME, csrfToken))
        .andExpect(status().isOk())
        .andReturn();

    // Capture the issued access token and the rotated refresh cookie
    var refreshJson = objectMapper.readTree(refreshRes.getResponse().getContentAsString());
    var accessToken = refreshJson.get("accessToken").asText();
    assertThat(accessToken).isNotBlank();

    var newRawRefreshToken = itHelper.extractCookieValue(
        refreshRes.getResponse().getHeaders(SET_COOKIE),
        refreshCookieName);
    assertThat(newRawRefreshToken).isNotBlank();
    assertThat(newRawRefreshToken).isNotEqualTo(oldRawRefreshToken);

    // Creating a room with a valid access token succeeds
    itHelper.createRoom("Bearer " + accessToken, Map.of("language", "IT"))
        .andExpect(status().isCreated());

    // Logout should revoke the current refresh token and clear the refresh cookie
    var logoutRes = mockMvc.perform(post("/auth/logout")
            .cookie(csrfCookie)
            .cookie(new jakarta.servlet.http.Cookie(refreshCookieName, newRawRefreshToken))
            .header(CSRF_HEADER_NAME, csrfToken))
        .andExpect(status().isNoContent())
        .andReturn();

    // Verify the refresh token is actually gone from the DB and the cookie is cleared
    var tokenHash = tokenHashing.sha256Hex(newRawRefreshToken);
    var tokenInDb = refreshTokenJpaRepository.findByTokenHash(tokenHash);
    assertThat(tokenInDb).isEmpty();

    var setCookies = logoutRes.getResponse().getHeaders(SET_COOKIE);
    var refreshSetCookieHeader = itHelper.findSetCookieHeader(setCookies, refreshCookieName);
    assertThat(refreshSetCookieHeader).contains("Max-Age=0");

    // Attempting to refresh again with the revoked cookie fails
    var refreshAfterLogoutRes = mockMvc.perform(post("/auth/refresh")
            .cookie(csrfCookie)
            .cookie(new jakarta.servlet.http.Cookie(refreshCookieName, newRawRefreshToken))
            .header(CSRF_HEADER_NAME, csrfToken))
        .andExpect(status().isUnauthorized())
        .andReturn();

    var refreshAfterLogoutJson = objectMapper
        .readTree(refreshAfterLogoutRes.getResponse().getContentAsString());
    assertThat(refreshAfterLogoutJson.get("code").asText()).isEqualTo("REFRESH_TOKEN_INVALID");
  }

  @Test
  void authFlow_expiredRefreshToken_returns401AndDeletesFromDb() throws Exception {
    // Create a real user + role and seed an already-expired refresh token directly into the DB
    var user = itHelper.createUser("22222222-2222-2222-2222-222222222222",
        "user-expired@test.com", "User");
    var rawRefreshToken = "expired-raw";
    var tokenHash = tokenHashing.sha256Hex(rawRefreshToken);

    var now = Instant.now();
    refreshTokenJpaRepository.save(new RefreshTokenEntity(
        UUID.randomUUID(), user, tokenHash, now.minusSeconds(7200), now.minusSeconds(3600)
    ));

    // Fetch a CSRF cookie so we can call the auth endpoints
    var csrfCookie = itHelper.fetchCsrfCookie();
    var csrfToken = csrfCookie.getValue();
    var refreshCookieName = securityProperties.refresh().cookieName();

    // Refreshing with an expired refresh token fails and must delete the token from DB
    var refreshRes = mockMvc.perform(post("/auth/refresh")
            .cookie(csrfCookie)
            .cookie(new jakarta.servlet.http.Cookie(refreshCookieName, rawRefreshToken))
            .header(CSRF_HEADER_NAME, csrfToken))
        .andExpect(status().isUnauthorized())
        .andReturn();

    var refreshJson = objectMapper.readTree(refreshRes.getResponse().getContentAsString());
    assertThat(refreshJson.get("code").asText()).isEqualTo("REFRESH_TOKEN_INVALID");

    var tokenInDb = refreshTokenJpaRepository.findByTokenHash(tokenHash);
    assertThat(tokenInDb).isEmpty();
  }
}
