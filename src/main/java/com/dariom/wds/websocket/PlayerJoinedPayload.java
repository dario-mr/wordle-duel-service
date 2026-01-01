package com.dariom.wds.websocket;

import java.util.List;

public record PlayerJoinedPayload(
    String playerId,
    List<String> players
) {

}
