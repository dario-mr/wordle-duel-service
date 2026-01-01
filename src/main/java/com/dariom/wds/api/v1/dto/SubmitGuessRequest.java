package com.dariom.wds.api.v1.dto;

import com.dariom.wds.api.v1.validation.ValidPlayerId;
import com.dariom.wds.api.v1.validation.ValidWord;
import com.fasterxml.jackson.annotation.JsonProperty;

public record SubmitGuessRequest(
    @ValidPlayerId
    @JsonProperty("playerId")
    String playerId,

    @ValidWord
    @JsonProperty("word")
    String word
) {

}
