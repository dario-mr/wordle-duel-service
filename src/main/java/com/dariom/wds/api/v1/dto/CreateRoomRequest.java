package com.dariom.wds.api.v1.dto;

import com.dariom.wds.api.v1.validation.ValidLanguage;
import com.dariom.wds.api.v1.validation.ValidPlayerId;
import com.fasterxml.jackson.annotation.JsonProperty;

public record CreateRoomRequest(
    @ValidPlayerId
    @JsonProperty("playerId")
    String playerId,

    @ValidLanguage
    @JsonProperty("language")
    String language
) {

}
