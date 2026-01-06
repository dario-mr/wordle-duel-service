package com.dariom.wds.it;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.util.Throwables.getRootCause;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@SpringBootTest(webEnvironment = RANDOM_PORT)
class WebSocketConfigIT {

  private static final Duration TIMEOUT = Duration.ofSeconds(5);

  @LocalServerPort
  private int port;

  @Test
  void websocketHandshake_allowedOrigin_connects() throws Exception {
    // Arrange
    var url = URI.create("ws://localhost:" + port + "/ws");
    var headers = new WebSocketHttpHeaders();
    headers.setOrigin("http://allowed-origin.test");
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
}

