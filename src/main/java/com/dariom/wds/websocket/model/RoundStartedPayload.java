package com.dariom.wds.websocket.model;

public record RoundStartedPayload(
    int roundNumber,
    int maxAttempts
) implements EventPayload {

}
