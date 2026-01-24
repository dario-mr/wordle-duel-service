package com.dariom.wds.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "room.cleanup")
public record RoomCleanupProperties(
    int retentionDays
) {

}
