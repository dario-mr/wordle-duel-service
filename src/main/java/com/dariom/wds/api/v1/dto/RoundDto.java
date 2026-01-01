package com.dariom.wds.api.v1.dto;

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
    @JsonProperty("finished")
    boolean finished
) {

}
