package com.dariom.wds.websocket;

public record RoundStartedPayload(
    int roundNumber,
    int maxAttempts
) {

}
