package com.dariom.wds.it;

import static com.dariom.wds.it.IntegrationTestHelper.CSRF_HEADER_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.SET_COOKIE;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthFlowIT extends AbstractRedisTest {

  private static final String ME_URL = "/api/v1/users/me";
  private static final String ROOMS_URL = "/api/v1/rooms";

  @Resource
  private ObjectMapper objectMapper;

  @Resource
  private MockMvc mockMvc;

  @Resource
  private IntegrationTestHelper itHelper;

  @Value("${server.servlet.session.cookie.name}")
  private String sessionCookieName;

  @Test
  void protectedEndpoint_withoutSession_returns401() throws Exception {
    // Act / Assert
    mockMvc.perform(get(ME_URL))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void protectedEndpoint_withSession_returnsCurrentUserAndRoles() throws Exception {
    // Arrange
    var user = itHelper.createUser("11111111-1111-1111-1111-111111111111", "user@test.com", "User");

    // Act / Assert
    mockMvc.perform(get(ME_URL).with(itHelper.userAuthentication(user)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(user.getId().toString()))
        .andExpect(jsonPath("$.roles[0]").value("USER"));
  }

  @Test
  void unsafeEndpoint_withoutCsrf_returns403() throws Exception {
    // Arrange
    var user = itHelper.createUser("22222222-2222-2222-2222-222222222222", "csrf@test.com", "CSRF");

    // Act / Assert
    mockMvc.perform(post(ROOMS_URL)
            .with(itHelper.userAuthentication(user))
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("language", "IT", "rounds", 5))))
        .andExpect(status().isForbidden());
  }

  @Test
  void unsafeEndpoint_withCsrf_succeeds() throws Exception {
    // Arrange
    var user = itHelper.createUser("33333333-3333-3333-3333-333333333333", "safe@test.com", "Safe");

    // Act / Assert
    itHelper.createRoom(itHelper.userAuthentication(user), Map.of("language", "IT", "rounds", 5))
        .andExpect(status().isCreated());
  }

  @Test
  void logout_invalidatesSessionAndClearsCookies() throws Exception {
    // Arrange
    var user = itHelper.createUser("44444444-4444-4444-4444-444444444444", "logout@test.com", "Logout");
    var authenticated = mockMvc.perform(get(ME_URL).with(itHelper.userAuthentication(user)))
        .andExpect(status().isOk())
        .andReturn();
    var sessionCookie = authenticated.getResponse().getCookie(sessionCookieName);
    assertThat(sessionCookie).isNotNull();
    mockMvc.perform(get(ME_URL).cookie(sessionCookie))
        .andExpect(status().isOk());
    var csrfCookie = itHelper.fetchCsrfCookie();

    // Act
    var logout = mockMvc.perform(post("/auth/logout")
            .cookie(sessionCookie, csrfCookie)
            .header(CSRF_HEADER_NAME, csrfCookie.getValue()))
        .andExpect(status().isNoContent())
        .andReturn();

    // Assert
    mockMvc.perform(get(ME_URL).cookie(sessionCookie))
        .andExpect(status().isUnauthorized());
    var setCookies = logout.getResponse().getHeaders(SET_COOKIE);
    assertThat(setCookies).anyMatch(cookie -> cookie.startsWith(sessionCookieName + "=")
        && cookie.contains("Max-Age=0"));
    assertThat(setCookies).anyMatch(cookie -> cookie.startsWith("WD-XSRF-TOKEN=")
        && cookie.contains("Max-Age=0"));
  }
}
