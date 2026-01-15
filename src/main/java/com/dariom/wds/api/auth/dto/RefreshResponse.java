package com.dariom.wds.api.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RefreshResponse(
    @JsonProperty("accessToken")
    String accessToken,
    @JsonProperty("expiresInSeconds")
    long expiresInSeconds) {

}
