package com.dariom.wds.api.v1.dto;

import com.dariom.wds.api.v1.validation.ValidLanguage;
import com.dariom.wds.domain.Language;
import com.dariom.wds.domain.RoomRounds;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record CreateRoomRequest(
    @ValidLanguage
    @JsonProperty("language")
    @Schema(implementation = Language.class)
    String language,
    @NotNull(message = "rounds is required")
    @JsonProperty("rounds")
    RoomRounds rounds
) {

}
