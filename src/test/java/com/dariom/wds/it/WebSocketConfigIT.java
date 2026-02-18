package com.dariom.wds.it;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.util.Throwables.getRootCause;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import com.dariom.wds.domain.Role;
import com.dariom.wds.persistence.entity.AppUserEntity;
import com.dariom.wds.persistence.entity.RoleEntity;
import com.dariom.wds.service.auth.JwtService;
import java.net.URI;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.messaging.WebSocketStompClient;

@SpringBootTest(webEnvironment = RANDOM_PORT)
class WebSocketConfigIT {

  private static final Duration TIMEOUT = Duration.ofSeconds(5);
  private static final String ALLOWED_ORIGIN = "http://allowed-origin.test";

  @LocalServerPort
  private int port;

  @Autowired
  private JwtService jwtService;

  @Test
  void websocketHandshake_allowedOrigin_connects() throws Exception {
    // Arrange
    var url = URI.create("ws://localhost:" + port + "/ws");
    var headers = new WebSocketHttpHeaders();
    headers.setOrigin(ALLOWED_ORIGIN);
    var client = new StandardWebSocketClient();
    var handler = new TextWebSocketHandler();

    // Act
    var session = client.execute(handler, headers, url)
        .get(TIMEOUT.toMillis(), MILLISECONDS);

    // Assert
    assertThat(session.isOpen()).isTrue();
    session.close();
  }

  @Test
  void websocketHandshake_disallowedOrigin_rejected() {
    // Arrange
    var url = URI.create("ws://localhost:" + port + "/ws");
    var headers = new WebSocketHttpHeaders();
    headers.setOrigin("http://not-allowed-origin.test");
    var client = new StandardWebSocketClient();
    var handler = new TextWebSocketHandler();

    // Act
    var thrown = catchThrowable(() -> client.execute(handler, headers, url)
        .get(TIMEOUT.toMillis(), MILLISECONDS));

    // Assert
    assertThat(thrown).isNotNull();

    var rootCause = getRootCause(thrown);
    assertThat(rootCause.getMessage()).contains("[403]");
  }

  @Test
  void websocketStompConnect_validToken_succeeds() throws Exception {
    // Arrange
    var stompClient = new WebSocketStompClient(new StandardWebSocketClient());
    var url = "ws://localhost:" + port + "/ws";

    var connectHeaders = new WebSocketHttpHeaders();
    connectHeaders.setOrigin(ALLOWED_ORIGIN);

    var stompHeaders = new StompHeaders();
    stompHeaders.add("Authorization", "Bearer " + createTestJwt());

    // Act
    var session = stompClient.connectAsync(url, connectHeaders, stompHeaders,
        new StompSessionHandlerAdapter() {
        }).get(TIMEOUT.toMillis(), MILLISECONDS);

    // Assert
    assertThat(session.isConnected()).isTrue();
    session.disconnect();
  }

  @Test
  void websocketStompConnect_noToken_rejected() {
    // Arrange
    var stompClient = new WebSocketStompClient(new StandardWebSocketClient());
    var url = "ws://localhost:" + port + "/ws";

    var connectHeaders = new WebSocketHttpHeaders();
    connectHeaders.setOrigin(ALLOWED_ORIGIN);

    var errorFuture = new CompletableFuture<Throwable>();

    // Act
    stompClient.connectAsync(url, connectHeaders, new StompHeaders(),
        new StompSessionHandlerAdapter() {
          @Override
          public void handleTransportError(StompSession session, Throwable exception) {
            errorFuture.complete(exception);
          }
        });

    // Assert
    var error = catchThrowable(() -> errorFuture.get(TIMEOUT.toMillis(), MILLISECONDS));
    if (error == null) {
      assertThat(errorFuture).isCompleted();
    }
  }

  private String createTestJwt() {
    var user = new AppUserEntity(
        UUID.randomUUID(), "ws-test@example.com", "google-sub", "WS Test User", "pictureUrl");
    user.addRole(new RoleEntity(Role.USER.name()));
    return jwtService.createAccessToken(user).token();
  }
}
