package com.dariom.wds.it;

import static com.dariom.wds.config.CacheConfig.DISPLAY_NAME_CACHE;
import static com.dariom.wds.config.CacheConfig.USER_PROFILE_CACHE;
import static com.dariom.wds.domain.Role.ADMIN;
import static com.dariom.wds.domain.Role.USER;
import static com.dariom.wds.service.auth.OAuthUserService.APP_USER_ID_CLAIM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.dariom.wds.domain.Role;
import com.dariom.wds.persistence.entity.AppUserEntity;
import com.dariom.wds.persistence.entity.RoleEntity;
import com.dariom.wds.persistence.repository.jpa.AppUserJpaRepository;
import com.dariom.wds.persistence.repository.jpa.RoleJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@Lazy
@Component
@RequiredArgsConstructor
class IntegrationTestHelper {

  public static final String CSRF_HEADER_NAME = "X-WD-XSRF-TOKEN";

  private static final String BASE_URL = "/api/v1/rooms";
  private static final String CSRF_COOKIE_NAME = "WD-XSRF-TOKEN";

  private final MockMvc mockMvc;
  private final ObjectMapper objectMapper;
  private final AppUserJpaRepository appUserJpaRepository;
  private final RoleJpaRepository roleJpaRepository;
  private final CacheManager cacheManager;

  RequestPostProcessor userAuthentication() {
    return asUser(testUser(USER));
  }

  RequestPostProcessor adminAuthentication() {
    return asUser(testUser(ADMIN));
  }

  RequestPostProcessor userAuthentication(AppUserEntity user) {
    return asUser(user);
  }

  AppUserEntity createUser(String userId, String email, String fullName) {
    var roleName = USER.name();
    var role = roleJpaRepository.findById(roleName)
        .orElseGet(() -> roleJpaRepository.save(new RoleEntity(roleName)));

    var user = new AppUserEntity(UUID.fromString(userId), email, "google-sub-" + userId, fullName,
        "pictureUrl");
    user.addRole(role);
    user.setDisplayName(fullName);

    var savedUser = appUserJpaRepository.save(user);
    evictUserCaches(userId);
    return savedUser;
  }

  ResultActions createRoom(RequestPostProcessor authentication, Object body) throws Exception {
    return postJson(authentication, BASE_URL, body);
  }

  ResultActions joinRoom(String roomId, RequestPostProcessor authentication) throws Exception {
    return postJson(authentication, BASE_URL + "/{roomId}/join", Map.of(), roomId);
  }

  ResultActions submitGuess(String roomId, RequestPostProcessor authentication, String word)
      throws Exception {
    return postJson(authentication, BASE_URL + "/{roomId}/guess", Map.of("word", word), roomId);
  }

  ResultActions startNextRound(String roomId, RequestPostProcessor authentication)
      throws Exception {
    return postJson(authentication, BASE_URL + "/{roomId}/next", Map.of(), roomId);
  }

  ResultActions getRoom(String roomId, RequestPostProcessor authentication) throws Exception {
    return mockMvc.perform(get(BASE_URL + "/{roomId}", roomId)
        .with(authentication));
  }

  ResultActions postJson(RequestPostProcessor authentication, String urlTemplate, Object body,
      Object... uriVars) throws Exception {
    return mockMvc.perform(post(urlTemplate, uriVars)
        .with(authentication)
        .with(csrf())
        .contentType(APPLICATION_JSON)
        .content(body instanceof String str ? str : objectMapper.writeValueAsString(body)));
  }

  Cookie fetchCsrfCookie() throws Exception {
    var response = mockMvc.perform(get("/actuator/health"))
        .andReturn()
        .getResponse();

    var csrfCookie = response.getCookie(CSRF_COOKIE_NAME);
    assertThat(csrfCookie).isNotNull();
    assertThat(csrfCookie.getValue()).isNotBlank();
    return csrfCookie;
  }

  String extractCookieValue(List<String> setCookies, String cookieName) {
    var header = findSetCookieHeader(setCookies, cookieName);

    var start = cookieName.length() + 1;
    var end = header.indexOf(';');
    if (end == -1) {
      end = header.length();
    }

    return header.substring(start, end);
  }

  String findSetCookieHeader(List<String> setCookies, String cookieName) {
    return setCookies.stream()
        .filter(Objects::nonNull)
        .filter(h -> h.startsWith(cookieName + "="))
        .findFirst()
        .orElseThrow(() ->
            new AssertionError("Missing Set-Cookie for " + cookieName + ": " + setCookies));
  }

  private AppUserEntity testUser(Role role) {
    var user = new AppUserEntity(
        UUID.randomUUID(), "test@example.com", "google-sub", "Test User", "pictureUrl");
    user.addRole(new RoleEntity(role.name()));
    return user;
  }

  private RequestPostProcessor asUser(AppUserEntity user) {
    var authorities = user.getRoles().stream()
        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
        .toList();
    var claims = Map.<String, Object>of(
        "sub", user.getGoogleSub(),
        "email", user.getEmail(),
        APP_USER_ID_CLAIM, user.getId().toString()
    );
    var now = Instant.parse("2025-01-01T00:00:00Z");
    var idToken = new OidcIdToken("test-id-token", now, now.plusSeconds(300), claims);
    var principal = new DefaultOidcUser(authorities, idToken, new OidcUserInfo(claims), "email");
    var token = new UsernamePasswordAuthenticationToken(principal, null, authorities);
    return authentication(token);
  }

  private void evictUserCaches(String userId) {
    evictCache(DISPLAY_NAME_CACHE, userId);
    evictCache(USER_PROFILE_CACHE, userId);
  }

  private void evictCache(String cacheName, String key) {
    var cache = cacheManager.getCache(cacheName);
    if (cache != null) {
      cache.evict(key);
    }
  }
}
