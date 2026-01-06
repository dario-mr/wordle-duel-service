package com.dariom.wds.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record GuessDto(
    @JsonProperty("word")
    String word,
    @JsonProperty("letters")
    List<LetterResultDto> letters,
    @JsonProperty("attemptNumber")
    int attemptNumber
) {

}
