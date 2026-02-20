package com.dariom.wds.domain;

import java.time.Instant;

public record UserProfile(
    String id,
    String fullName,
    String displayName,
    String pictureUrl,
    Instant createdOn
) {

}
