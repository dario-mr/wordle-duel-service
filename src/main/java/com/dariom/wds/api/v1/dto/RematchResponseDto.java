package com.dariom.wds.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RematchResponseDto(
    @JsonProperty("started")
    boolean started
) {

}
