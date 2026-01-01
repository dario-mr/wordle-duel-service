package com.dariom.wds.websocket;

public record RoomEvent(
    String type,
    Object payload
) {

}
