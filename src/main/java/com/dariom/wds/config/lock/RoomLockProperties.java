package com.dariom.wds.config.lock;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "room.lock")
public record RoomLockProperties(
    Duration acquireTimeout
) {

}
