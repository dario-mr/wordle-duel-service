package com.dariom.wds.persistence.repository.jpa.projection;

import com.dariom.wds.domain.LetterStatus;
import com.dariom.wds.domain.RoundPlayerStatus;
import com.dariom.wds.domain.RoundStatus;

public record RoundFlatRowView(
    long roundId,
    String roomId,
    int roundNumber,
    int maxAttempts,
    RoundStatus roundStatus,
    String targetWord,
    String statusPlayerId,
    RoundPlayerStatus playerStatus,
    long guessId,
    String guessPlayerId,
    String guessWord,
    Integer attemptNumber,
    Integer letterOrder,
    Character letter,
    LetterStatus letterStatus
) {

}
