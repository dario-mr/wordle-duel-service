package com.dariom.wds.api.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record UserDto(
    @JsonProperty("id")
    String id,
    @JsonProperty("fullName")
    String fullName,
    @JsonProperty("displayName")
    String displayName,
    @JsonProperty("pictureUrl")
    String pictureUrl,
    @JsonProperty("createdOn")
    Instant createdOn
) {

}
