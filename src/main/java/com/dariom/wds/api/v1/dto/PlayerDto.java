package com.dariom.wds.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PlayerDto(
    @JsonProperty("id")
    String id,
    @JsonProperty("score")
    int score,
    @JsonProperty("displayName")
    String displayName
) {

}
