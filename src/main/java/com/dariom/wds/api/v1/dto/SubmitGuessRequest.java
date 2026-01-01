package com.dariom.wds.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SubmitGuessRequest(
    @JsonProperty("playerId")
    String playerId,
    @JsonProperty("word")
    String word
) {

}
