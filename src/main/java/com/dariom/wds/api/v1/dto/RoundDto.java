package com.dariom.wds.api.v1.dto;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.dariom.wds.domain.RoundStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public record RoundDto(
    @JsonProperty("roundNumber")
    int roundNumber,
    @JsonProperty("maxAttempts")
    int maxAttempts,
    @JsonProperty("guessesByPlayerId")
    Map<String, List<GuessDto>> guessesByPlayerId,
    @JsonProperty("statusByPlayerId")
    Map<String, String> statusByPlayerId,
    @JsonProperty("roundStatus")
    RoundStatus roundStatus,
    @JsonInclude(NON_NULL)
    @JsonProperty("solution")
    String solution
) {

}
