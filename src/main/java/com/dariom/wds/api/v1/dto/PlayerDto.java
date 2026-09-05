package com.dariom.wds.api.v1.dto;

public record PlayerDto(
    String id,
    int wins,
    Integer matchScore,
    String displayName
) {

}
