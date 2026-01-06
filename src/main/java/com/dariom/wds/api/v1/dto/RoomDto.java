package com.dariom.wds.api.v1.dto;

import com.dariom.wds.domain.Language;
import com.dariom.wds.domain.RoomStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record RoomDto(
    @JsonProperty("id")
    String id,
    @JsonProperty("language")
    Language language,
    @JsonProperty("status")
    RoomStatus status,
    @JsonProperty("players")
    List<PlayerDto> players,
    @JsonProperty("currentRound")
    RoundDto currentRound
) {

}
