package com.dariom.wds.websocket;

import java.util.Map;

public record ScoresUpdatedPayload(
    Map<String, Integer> scores
) {

}
