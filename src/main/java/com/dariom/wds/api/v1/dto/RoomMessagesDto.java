package com.dariom.wds.api.v1.dto;

import java.util.List;

public record RoomMessagesDto(List<RoomMessageDto> messages, long unreadCount) {
}
