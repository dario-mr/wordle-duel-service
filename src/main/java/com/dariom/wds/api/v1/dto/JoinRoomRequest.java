package com.dariom.wds.api.v1.dto;

import com.dariom.wds.api.v1.validation.ValidPlayerId;
import com.fasterxml.jackson.annotation.JsonProperty;

public record JoinRoomRequest(
    @ValidPlayerId
    @JsonProperty("playerId")
    String playerId
) {

}
