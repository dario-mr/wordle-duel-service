package com.dariom.wds.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserMeDto(
    @JsonProperty("id")
    String id,
    @JsonProperty("fullName")
    String fullName,
    @JsonProperty("displayName")
    String displayName,
    @JsonProperty("pictureUrl")
    String pictureUrl
) {

}
