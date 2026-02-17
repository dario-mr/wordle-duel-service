package com.dariom.wds.config.ws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.messaging.simp.stomp.StompCommand.CONNECT;
import static org.springframework.messaging.simp.stomp.StompCommand.SUBSCRIBE;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthInterceptorTest {

  @Mock
  private JwtDecoder jwtDecoder;

  @Mock
  private JwtAuthenticationConverter jwtAuthenticationConverter;

  @Mock
  private Jwt jwt;

  @Mock
  private AbstractAuthenticationToken authentication;

  @InjectMocks
  private WebSocketAuthInterceptor interceptor;

  @Test
  void preSend_connectWithValidToken_setsUser() {
    // Arrange
    var message = createStompMessage(CONNECT, "Bearer valid-token");
    when(jwtDecoder.decode(any())).thenReturn(jwt);
    when(jwtAuthenticationConverter.convert(any())).thenReturn(authentication);

    // Act
    var result = interceptor.preSend(message, null);

    // Assert
    assertThat(result).isNotNull();
    verify(jwtDecoder).decode("valid-token");
    verify(jwtAuthenticationConverter).convert(jwt);
  }

  @Test
  void preSend_connectWithNoAuthHeader_throwsMessageDeliveryException() {
    // Arrange
    var message = createStompMessage(CONNECT, null);

    // Act
    var thrown = catchThrowable(() -> interceptor.preSend(message, null));

    // Assert
    assertThat(thrown)
        .isInstanceOf(MessageDeliveryException.class)
        .hasMessage("Missing or invalid Authorization header");
    verify(jwtDecoder, never()).decode(any());
  }

  @Test
  void preSend_connectWithInvalidPrefix_throwsMessageDeliveryException() {
    // Arrange
    var message = createStompMessage(CONNECT, "Basic some-credentials");

    // Act
    var thrown = catchThrowable(() -> interceptor.preSend(message, null));

    // Assert
    assertThat(thrown)
        .isInstanceOf(MessageDeliveryException.class)
        .hasMessage("Missing or invalid Authorization header");
  }

  @Test
  void preSend_connectWithExpiredToken_throwsMessageDeliveryException() {
    // Arrange
    var message = createStompMessage(CONNECT, "Bearer expired-token");
    when(jwtDecoder.decode(any())).thenThrow(new JwtException("Token expired"));

    // Act
    var thrown = catchThrowable(() -> interceptor.preSend(message, null));

    // Assert
    assertThat(thrown)
        .isInstanceOf(MessageDeliveryException.class)
        .hasMessage("Invalid JWT token");
  }

  @Test
  void preSend_nonConnectCommand_passesThrough() {
    // Arrange
    var message = createStompMessage(SUBSCRIBE, null);

    // Act
    var result = interceptor.preSend(message, null);

    // Assert
    assertThat(result).isSameAs(message);
    verify(jwtDecoder, never()).decode(any());
  }

  private static Message<byte[]> createStompMessage(StompCommand command, String authorization) {
    var accessor = StompHeaderAccessor.create(command);
    accessor.setLeaveMutable(true);
    if (authorization != null) {
      accessor.addNativeHeader("Authorization", authorization);
    }
    return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
  }
}
