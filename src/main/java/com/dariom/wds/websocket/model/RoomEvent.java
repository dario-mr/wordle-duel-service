package com.dariom.wds.websocket.model;

public record RoomEvent(
    EventType type,
    EventPayload payload
) {

}
