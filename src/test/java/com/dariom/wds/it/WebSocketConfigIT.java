package com.dariom.wds.it;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.util.Throwables.getRootCause;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.annotation.Resource;
import jakarta.servlet.http.Cookie;
import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.messaging.WebSocketStompClient;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureMockMvc
class WebSocketConfigIT extends AbstractRedisTest {

  private static final Duration TIMEOUT = Duration.ofSeconds(5);
  private static final String ALLOWED_ORIGIN = "http://allowed-origin.test";

  @LocalServerPort
  private int port;

  @Resource
  private MockMvc mockMvc;

  @Resource
  private IntegrationTestHelper itHelper;

  @Value("${server.servlet.session.cookie.name}")
  private String sessionCookieName;

  @Test
  void websocketHandshake_withoutSession_rejected() {
    // Arrange
    var url = URI.create("ws://localhost:" + port + "/ws");
    var headers = new WebSocketHttpHeaders();
    headers.setOrigin(ALLOWED_ORIGIN);
    var client = new StandardWebSocketClient();

    // Act
    var thrown = catchThrowable(() -> client.execute(new TextWebSocketHandler(), headers, url)
        .get(TIMEOUT.toMillis(), MILLISECONDS));

    // Assert
    assertThat(getRootCause(thrown).getMessage()).contains("[401]");
  }

  @Test
  void websocketHandshake_disallowedOrigin_rejected() throws Exception {
    // Arrange
    var user = itHelper.createUser("22222222-2222-2222-2222-222222222222", "ws-origin@test.com", "WebSocket");
    var authenticated = mockMvc.perform(get("/api/v1/users/me")
            .with(itHelper.userAuthentication(user)))
        .andReturn();
    var sessionCookie = authenticated.getResponse().getCookie(sessionCookieName);
    assertThat(sessionCookie).isNotNull();
    var url = URI.create("ws://localhost:" + port + "/ws");
    var headers = new WebSocketHttpHeaders();
    headers.setOrigin("http://not-allowed-origin.test");
    headers.add(HttpHeaders.COOKIE, cookieHeader(sessionCookie));
    var client = new StandardWebSocketClient();

    // Act
    var thrown = catchThrowable(() -> client.execute(new TextWebSocketHandler(), headers, url)
        .get(TIMEOUT.toMillis(), MILLISECONDS));

    // Assert
    assertThat(getRootCause(thrown).getMessage()).contains("[403]");
  }

  @Test
  void websocketStompConnect_withSession_succeeds() throws Exception {
    // Arrange
    var user = itHelper.createUser("11111111-1111-1111-1111-111111111111", "ws@test.com", "WebSocket");
    var authenticated = mockMvc.perform(get("/api/v1/users/me")
            .with(itHelper.userAuthentication(user)))
        .andExpect(status().isOk())
        .andReturn();
    var sessionCookie = authenticated.getResponse().getCookie(sessionCookieName);
    assertThat(sessionCookie).isNotNull();

    var stompClient = new WebSocketStompClient(new StandardWebSocketClient());
    var url = "ws://localhost:" + port + "/ws";
    var connectHeaders = new WebSocketHttpHeaders();
    connectHeaders.setOrigin(ALLOWED_ORIGIN);
    connectHeaders.add(HttpHeaders.COOKIE, cookieHeader(sessionCookie));

    // Act
    var session = stompClient.connectAsync(url, connectHeaders, new StompHeaders(),
        new StompSessionHandlerAdapter() {}).get(TIMEOUT.toMillis(), MILLISECONDS);

    // Assert
    assertThat(session.isConnected()).isTrue();
    session.disconnect();
  }

  private static String cookieHeader(Cookie cookie) {
    return cookie.getName() + "=" + cookie.getValue();
  }
}
