package com.dariom.wds.persistence.repository.jpa.projection;

import com.dariom.wds.domain.LetterStatus;

public record GuessLetterView(
    long roundId,
    long guessId,
    String playerId,
    String word,
    int attemptNumber,
    Integer letterOrder,
    Character letter,
    LetterStatus letterStatus
) {

}
