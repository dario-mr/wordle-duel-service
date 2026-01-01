package com.dariom.wds.api.v1.dto;

import com.dariom.wds.domain.Language;
import com.fasterxml.jackson.annotation.JsonProperty;

public record CreateRoomRequest(
    @JsonProperty("playerId")
    String playerId,
    @JsonProperty("language")
    Language language
) {

}
