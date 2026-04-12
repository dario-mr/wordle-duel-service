package com.dariom.wds.persistence.repository.jpa.projection;

import com.dariom.wds.domain.RoundStatus;

public record RoundHeaderView(
    long roundId,
    String roomId,
    int roundNumber,
    int maxAttempts,
    RoundStatus roundStatus,
    String targetWord
) {

}
