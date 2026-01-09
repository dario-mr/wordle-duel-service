package com.dariom.wds.websocket.model;

public record PlayerReadyPayload(String playerId) implements EventPayload {

}
