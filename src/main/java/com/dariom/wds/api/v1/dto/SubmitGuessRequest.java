package com.dariom.wds.api.v1.dto;

import com.dariom.wds.api.v1.validation.ValidWord;
import com.fasterxml.jackson.annotation.JsonProperty;

public record SubmitGuessRequest(
    @ValidWord
    @JsonProperty("word")
    String word
) {

}
