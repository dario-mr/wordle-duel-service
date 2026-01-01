package com.dariom.wds.websocket.model;

import java.util.Map;

public record ScoresUpdatedPayload(
    Map<String, Integer> scores
) implements EventPayload {

}
