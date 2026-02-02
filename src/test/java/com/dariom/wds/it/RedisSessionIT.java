package com.dariom.wds.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS;

import jakarta.annotation.Resource;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@DirtiesContext(classMode = AFTER_CLASS)
class RedisSessionIT {

  @Container
  static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
      .withExposedPorts(6379)
      .waitingFor(Wait.forListeningPort());

  @DynamicPropertySource
  static void registerRedisProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.session.store-type", () -> "redis");
    registry.add("spring.data.redis.host", redis::getHost);
    registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    registry.add("management.health.redis.enabled", () -> "true");
  }

  @LocalServerPort
  private int port;

  @Resource
  private StringRedisTemplate stringRedisTemplate;

  private final HttpClient httpClient = HttpClient.newBuilder()
      .followRedirects(Redirect.NEVER)
      .build();

  @AfterEach
  void clearRedis() {
    stringRedisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
  }

  @Test
  void oauth2Authorization_google_createsSessionInRedis() throws Exception {
    // Arrange
    var request = HttpRequest.newBuilder(uri("/oauth2/authorization/google"))
        .GET()
        .build();

    // Act
    var response = httpClient.send(request, BodyHandlers.ofString());

    // Assert
    assertThat(response.statusCode()).isEqualTo(302);

    var setCookies = response.headers().allValues("set-cookie");
    var sessionCookie = extractCookieValue(setCookies, "SESSION");
    assertThat(sessionCookie).isNotBlank();

    var sessionKeys = waitForSessionKeys();
    assertThat(sessionKeys).isNotEmpty();
  }

  private List<String> waitForSessionKeys() throws InterruptedException {
    for (int attempt = 0; attempt < 20; attempt++) {
      var keys = stringRedisTemplate.keys("spring:session:sessions:*");
      if (!keys.isEmpty()) {
        return keys.stream().sorted().toList();
      }

      Thread.sleep(50);
    }

    return List.of();
  }

  private URI uri(String path) {
    return URI.create("http://localhost:" + port + path);
  }

  private static String extractCookieValue(List<String> setCookies, String cookieName) {
    var header = setCookies.stream()
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
