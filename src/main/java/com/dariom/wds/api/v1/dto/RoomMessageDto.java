package com.dariom.wds.api.v1.dto;

import com.dariom.wds.domain.RoomMessagePreset;
import java.time.Instant;

public record RoomMessageDto(
    Long id,
    String senderPlayerId,
    RoomMessagePreset preset,
    Instant createdAt
) {
}
