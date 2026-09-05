package com.dariom.wds.domain;

public record Player(
    String id,
    int wins,
    Integer matchScore,
    String displayName
) {

}
