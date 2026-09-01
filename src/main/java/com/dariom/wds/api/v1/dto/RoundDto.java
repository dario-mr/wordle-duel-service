package com.dariom.wds.api.v1.dto;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.dariom.wds.domain.RoundStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record RoundDto(
    @JsonProperty("roundNumber")
    int roundNumber,
    @JsonProperty("maxAttempts")
    int maxAttempts,
    @JsonProperty("guesses")
    List<GuessDto> guesses,
    @JsonProperty("playerStatus")
    String playerStatus,
    @JsonProperty("roundStatus")
    RoundStatus roundStatus,
    @JsonInclude(NON_NULL)
    @JsonProperty("solution")
    String solution
) {

}
