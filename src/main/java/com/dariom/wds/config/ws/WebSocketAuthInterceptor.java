package com.dariom.wds.config.ws;

import static org.springframework.messaging.simp.stomp.StompCommand.CONNECT;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtDecoder jwtDecoder;
  private final JwtAuthenticationConverter jwtAuthenticationConverter;

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    var accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    if (accessor == null || accessor.getCommand() != CONNECT) {
      return message;
    }

    var authorization = accessor.getFirstNativeHeader("Authorization");
    if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
      throw new MessageDeliveryException("Missing or invalid Authorization header");
    }

    var token = authorization.substring(BEARER_PREFIX.length());
    try {
      var jwt = jwtDecoder.decode(token);
      var authentication = jwtAuthenticationConverter.convert(jwt);
      accessor.setUser(authentication);
    } catch (JwtException e) {
      log.debug("WebSocket CONNECT JWT validation failed: {}", e.getMessage());
      throw new MessageDeliveryException("Invalid JWT token");
    }

    return message;
  }
}
