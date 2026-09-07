package com.dariom.wds.api.admin.dto;

public record AdminPlayerDto(
    String id,
    int wins,
    int matchScore,
    String displayName,
    Integer currentRoundNumber
) {

}
