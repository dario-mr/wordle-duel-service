package com.dariom.wds.websocket.model;

public record RematchStartedPayload(
    String roomId
) implements EventPayload {

}
