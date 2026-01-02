package com.dariom.wds.domain;

import java.util.List;
import java.util.Map;

public record Room(
    String id,
    Language language,
    RoomStatus status,
    List<String> players,
    Map<String, Integer> scoresByPlayerId,
    Round currentRound
) {
}
