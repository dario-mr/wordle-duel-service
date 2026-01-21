package com.dariom.wds.domain;

public record UserProfile(
    String id,
    String fullName,
    String displayName,
    String pictureUrl
) {

}
