package com.dariom.wds.websocket.model;

import java.util.List;

public record PlayerJoinedPayload(
    String playerId,
    List<String> players
) implements EventPayload {

}
