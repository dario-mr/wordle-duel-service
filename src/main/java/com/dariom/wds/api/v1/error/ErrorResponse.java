package com.dariom.wds.api.v1.error;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ErrorResponse(
    @JsonProperty("code")
    ErrorCode code,
    @JsonProperty("message")
    String message
) {

}
