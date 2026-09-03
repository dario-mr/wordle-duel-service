package com.dariom.wds.api.v1.dto;

import com.dariom.wds.domain.RoomMessagePreset;
import jakarta.validation.constraints.NotNull;

public record SendRoomMessageRequest(@NotNull RoomMessagePreset preset) {
}
