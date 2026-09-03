package com.dariom.wds.websocket.model;

import com.dariom.wds.domain.RoomMessagePreset;
import java.time.Instant;

public record RoomMessagePayload(
    Long id,
    String senderPlayerId,
    RoomMessagePreset preset,
    Instant createdAt
) implements EventPayload {
}
