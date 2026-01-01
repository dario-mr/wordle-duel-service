package com.dariom.wds.api.v1.dto;

import com.dariom.wds.domain.Language;
import com.dariom.wds.domain.RoomStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public record RoomDto(
    @JsonProperty("id")
    String id,
    @JsonProperty("language")
    Language language,
    @JsonProperty("status")
    RoomStatus status,
    @JsonProperty("players")
    List<String> players,
    @JsonProperty("scores")
    Map<String, Integer> scoresByPlayerId,
    @JsonProperty("currentRound")
    RoundDto currentRound
) {

}
