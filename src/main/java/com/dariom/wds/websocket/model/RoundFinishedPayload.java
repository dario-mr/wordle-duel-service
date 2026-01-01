package com.dariom.wds.websocket.model;

public record RoundFinishedPayload(
    int roundNumber
) implements EventPayload {

}
