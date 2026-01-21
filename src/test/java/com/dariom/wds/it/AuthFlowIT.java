package com.dariom.wds.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import com.dariom.wds.config.security.SecurityProperties;
import com.dariom.wds.persistence.entity.AppUserEntity;
import com.dariom.wds.persistence.entity.RefreshTokenEntity;
import com.dariom.wds.persistence.entity.RoleEntity;
import com.dariom.wds.persistence.repository.jpa.AppUserJpaRepository;
import com.dariom.wds.persistence.repository.jpa.RefreshTokenJpaRepository;
import com.dariom.wds.persistence.repository.jpa.RoleJpaRepository;
import com.dariom.wds.service.auth.RefreshTokenService;
import com.dariom.wds.service.auth.TokenHashing;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(webEnvironment = RANDOM_PORT)
class AuthFlowIT {

  private static final String CSRF_COOKIE_NAME = "WD-XSRF-TOKEN";
  private static final String CSRF_HEADER_NAME = "X-WD-XSRF-TOKEN";
  private static final String ROOMS_URL = "/api/v1/rooms";

  @LocalServerPort
  private int port;

  @Resource
  private ObjectMapper objectMapper;

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

  @Resource
  private PlatformTransactionManager transactionManager;

  private final HttpClient httpClient = HttpClient.newHttpClient();

  @Test
  void authFlow_refreshAllowsApiCall_logoutRevokesAndPreventsRefresh() throws Exception {
    // Creating a room without a Bearer token returns 401
    var createRoomRequest = objectMapper.writeValueAsString(Map.of("language", "IT"));
    var createRoomUnauth = postJson(ROOMS_URL, createRoomRequest, Map.of());
    assertThat(createRoomUnauth.statusCode()).isEqualTo(401);

    // Create a real user + role and seed a refresh token directly into the DB
    var user = createUserWithRole("user@test.com", "USER");
    var oldRawRefreshToken = refreshTokenService.createRefreshToken(user);

    // Fetch a CSRF cookie (XSRF-TOKEN) so we can call the auth endpoints
    var csrfToken = fetchCsrfToken();
    var refreshCookieName = securityProperties.refresh().cookieName();

    // Exchange refresh cookie (+ CSRF header) for a short-lived access token
    var refreshRes = post("/auth/refresh",
        Map.of(
            CSRF_HEADER_NAME, csrfToken,
            "Cookie", cookieHeader(csrfToken, refreshCookieName, oldRawRefreshToken)
        ));
    assertThat(refreshRes.statusCode()).isEqualTo(200);

    // Capture the issued access token and the rotated refresh cookie
    var refreshJson = objectMapper.readTree(refreshRes.body());
    var accessToken = refreshJson.get("accessToken").asText();
    assertThat(accessToken).isNotBlank();

    var newRawRefreshToken = extractCookieValue(refreshRes.headers().allValues("set-cookie"),
        refreshCookieName);
    assertThat(newRawRefreshToken).isNotBlank();
    assertThat(newRawRefreshToken).isNotEqualTo(oldRawRefreshToken);

    // Creating a room with a valid access token succeeds
    var createRoomAuth = postJson(ROOMS_URL, createRoomRequest, Map.of(
        "Authorization", "Bearer " + accessToken
    ));
    assertThat(createRoomAuth.statusCode()).isEqualTo(201);

    // Logout should revoke the current refresh token and clear the refresh cookie
    var logoutRes = post("/auth/logout",
        Map.of(
            CSRF_HEADER_NAME, csrfToken,
            "Cookie", cookieHeader(csrfToken, refreshCookieName, newRawRefreshToken)
        ));
    assertThat(logoutRes.statusCode()).isEqualTo(204);

    // Verify the refresh token is actually gone from the DB and the cookie is cleared
    var tokenHash = tokenHashing.sha256Hex(newRawRefreshToken);
    var tokenInDb = inTx(() -> refreshTokenJpaRepository.findByTokenHash(tokenHash));
    assertThat(tokenInDb).isEmpty();

    var setCookies = logoutRes.headers().allValues("set-cookie");
    var refreshSetCookieHeader = findSetCookieHeader(setCookies, refreshCookieName);
    assertThat(refreshSetCookieHeader).contains("Max-Age=0");

    // Attempting to refresh again with the revoked cookie fails
    var refreshAfterLogoutRes = post("/auth/refresh",
        Map.of(
            CSRF_HEADER_NAME, csrfToken,
            "Cookie", cookieHeader(csrfToken, refreshCookieName, newRawRefreshToken)
        ));

    assertThat(refreshAfterLogoutRes.statusCode()).isEqualTo(401);
    var refreshAfterLogoutJson = objectMapper.readTree(refreshAfterLogoutRes.body());
    assertThat(refreshAfterLogoutJson.get("code").asText()).isEqualTo("REFRESH_TOKEN_INVALID");
  }

  @Test
  void authFlow_expiredRefreshToken_returns401AndDeletesFromDb() throws Exception {
    // Create a real user + role and seed an already-expired refresh token directly into the DB
    var user = createUserWithRole("user-expired@test.com", "USER");
    var rawRefreshToken = "expired-raw";
    var tokenHash = tokenHashing.sha256Hex(rawRefreshToken);

    var now = Instant.now();
    inTx(() -> refreshTokenJpaRepository.save(new RefreshTokenEntity(
        UUID.randomUUID(), user, tokenHash, now.minusSeconds(7200), now.minusSeconds(3600)
    )));

    // Fetch a CSRF cookie so we can call the auth endpoints
    var csrfToken = fetchCsrfToken();
    var refreshCookieName = securityProperties.refresh().cookieName();

    // Refreshing with an expired refresh token fails and must delete the token from DB
    var refreshRes = post("/auth/refresh",
        Map.of(
            CSRF_HEADER_NAME, csrfToken,
            "Cookie", cookieHeader(csrfToken, refreshCookieName, rawRefreshToken)
        ));

    assertThat(refreshRes.statusCode()).isEqualTo(401);
    var refreshJson = objectMapper.readTree(refreshRes.body());
    assertThat(refreshJson.get("code").asText()).isEqualTo("REFRESH_TOKEN_INVALID");

    var tokenInDb = inTx(() -> refreshTokenJpaRepository.findByTokenHash(tokenHash));
    assertThat(tokenInDb).isEmpty();
  }

  private String fetchCsrfToken() throws Exception {
    var res = get("/actuator/health");
    assertThat(res.statusCode()).isEqualTo(200);

    return extractCookieValue(res.headers().allValues("set-cookie"), CSRF_COOKIE_NAME);
  }

  private HttpResponse<String> get(String path) throws Exception {
    return httpClient.send(HttpRequest.newBuilder(uri(path)).GET().build(),
        BodyHandlers.ofString());
  }

  private HttpResponse<String> post(String path, Map<String, String> headers) throws Exception {
    var builder = HttpRequest.newBuilder(uri(path))
        .POST(HttpRequest.BodyPublishers.noBody());
    headers.forEach(builder::header);

    return httpClient.send(builder.build(), BodyHandlers.ofString());
  }

  private HttpResponse<String> postJson(String path, String body, Map<String, String> headers)
      throws Exception {
    var builder = HttpRequest.newBuilder(uri(path))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body));

    headers.forEach(builder::header);

    return httpClient.send(builder.build(), BodyHandlers.ofString());
  }

  private <T> T inTx(Supplier<T> operation) {
    var result = new TransactionTemplate(transactionManager).execute(status -> operation.get());
    assertThat(result).isNotNull();
    return result;
  }

  private URI uri(String path) {
    return URI.create("http://localhost:" + port + path);
  }

  private AppUserEntity createUserWithRole(String email, String roleName) {
    var role = roleJpaRepository.findById(roleName)
        .orElseGet(() -> roleJpaRepository.save(new RoleEntity(roleName)));
    var googleSub = "google-sub-" + UUID.randomUUID();
    var user = new AppUserEntity(UUID.randomUUID(), email, googleSub, "User Test", "pictureUrl");
    user.addRole(role);

    return appUserJpaRepository.save(user);
  }

  private static String cookieHeader(String csrfToken, String refreshCookieName,
      String refreshToken) {
    return CSRF_COOKIE_NAME + "=" + csrfToken + "; " + refreshCookieName + "=" + refreshToken;
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
