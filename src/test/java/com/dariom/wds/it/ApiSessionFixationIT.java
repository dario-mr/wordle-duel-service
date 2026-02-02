package com.dariom.wds.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.annotation.Resource;
import jakarta.servlet.http.Cookie;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureMockMvc
@DirtiesContext(classMode = AFTER_CLASS)
class ApiSessionFixationIT {

  @Container
  static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
      .withExposedPorts(6379)
      .waitingFor(Wait.forListeningPort());

  @DynamicPropertySource
  static void registerRedisProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.session.store-type", () -> "redis");
    registry.add("spring.data.redis.host", redis::getHost);
    registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
  }

  @Resource
  private MockMvc mockMvc;

  @Resource
  private IntegrationTestHelper itHelper;

  @Resource
  private StringRedisTemplate stringRedisTemplate;

  @AfterEach
  void clearRedis() {
    stringRedisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
  }

  @Test
  void me_withBearerAndSessionCookie_doesNotChangeSessionIdOrSetSessionCookie() throws Exception {
    // Arrange
    var user = itHelper.createUser("11111111-1111-1111-1111-111111111111", "user@test.com", "User");
    var bearer = itHelper.bearer(user);

    var oauthStart = mockMvc.perform(get("/oauth2/authorization/google"))
        .andExpect(status().is3xxRedirection())
        .andReturn();

    var setCookies = oauthStart.getResponse().getHeaders("Set-Cookie").stream().toList();
    var sessionId = extractCookieValue(setCookies, "SESSION");
    assertThat(sessionId).isNotBlank();

    var sessionCookie = new Cookie("SESSION", sessionId);

    // Act
    var meResponse1 = mockMvc.perform(get("/api/v1/users/me")
            .header("Authorization", bearer)
            .cookie(sessionCookie))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse();

    var meResponse2 = mockMvc.perform(get("/api/v1/users/me")
            .header("Authorization", bearer)
            .cookie(sessionCookie))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse();

    // Assert
    assertThat(meResponse1.getHeaders("Set-Cookie"))
        .noneMatch(h -> h.startsWith("SESSION="));
    assertThat(meResponse2.getHeaders("Set-Cookie"))
        .noneMatch(h -> h.startsWith("SESSION="));
  }

  private static String extractCookieValue(List<String> setCookies, String cookieName) {
    var header = setCookies.stream()
        .filter(h -> h != null)
        .filter(h -> h.startsWith(cookieName + "="))
        .findFirst()
        .orElseThrow(
            () -> new AssertionError("Missing Set-Cookie for " + cookieName + ": " + setCookies));

    var start = cookieName.length() + 1;
    var end = header.indexOf(';');
    if (end == -1) {
      end = header.length();
    }

    return header.substring(start, end);
  }
}
