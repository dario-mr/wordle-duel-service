package com.dariom.wds.api.common;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ErrorResponse(
    @JsonProperty("code")
    ErrorCode code,
    @JsonProperty("message")
    String message
) {

}
