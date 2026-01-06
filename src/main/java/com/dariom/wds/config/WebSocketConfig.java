package com.dariom.wds.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@RequiredArgsConstructor
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  private final WebSocketProperties webSocketProperties;

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    var endpoint = registry.addEndpoint("/ws");

    var allowedOrigins = webSocketProperties.allowedOrigins();
    if (allowedOrigins != null && !allowedOrigins.isEmpty()) {
      endpoint.setAllowedOrigins(allowedOrigins.toArray(String[]::new));
    }
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.enableSimpleBroker("/topic");
    registry.setApplicationDestinationPrefixes("/app");
  }
}
