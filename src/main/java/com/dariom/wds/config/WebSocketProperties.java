package com.dariom.wds.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "wds.websocket")
public record WebSocketProperties(
    List<String> allowedOrigins
) {

}
