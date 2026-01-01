package com.dariom.wds.api.v1.dto;

import com.dariom.wds.api.v1.validation.ValidLanguage;
import com.dariom.wds.api.v1.validation.ValidPlayerId;
import com.dariom.wds.domain.Language;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

public record CreateRoomRequest(
    @ValidPlayerId
    @JsonProperty("playerId")
    String playerId,

    @ValidLanguage
    @JsonProperty("language")
    @Schema(implementation = Language.class)
    String language
) {

}
