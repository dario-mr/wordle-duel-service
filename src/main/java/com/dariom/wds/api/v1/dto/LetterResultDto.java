package com.dariom.wds.api.v1.dto;

import com.dariom.wds.domain.LetterStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

public record LetterResultDto(
    @JsonProperty("letter")
    char letter,
    @JsonProperty("status")
    LetterStatus status
) {

}
