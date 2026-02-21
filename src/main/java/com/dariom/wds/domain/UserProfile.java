package com.dariom.wds.domain;

import java.time.Instant;

public record UserProfile(
    String id,
    String email,
    String fullName,
    String displayName,
    String pictureUrl,
    Instant createdOn
) {

}
