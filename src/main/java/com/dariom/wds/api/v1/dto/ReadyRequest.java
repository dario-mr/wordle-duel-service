package com.dariom.wds.api.v1.dto;

import com.dariom.wds.api.v1.validation.ValidRoundNumber;
import com.fasterxml.jackson.annotation.JsonProperty;

public record ReadyRequest(
    @ValidRoundNumber
    @JsonProperty("roundNumber")
    Integer roundNumber
) {

}
