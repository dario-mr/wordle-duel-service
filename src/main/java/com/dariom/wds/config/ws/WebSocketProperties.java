package com.dariom.wds.config.ws;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.websocket")
public record WebSocketProperties(
    List<String> allowedOrigins
) {

}
