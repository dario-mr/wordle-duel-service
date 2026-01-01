package com.dariom.wds.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "wordle")
public record WordleProperties(
    int maxAttempts
) {

}
