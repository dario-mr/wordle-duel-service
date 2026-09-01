package com.dariom.wds.domain;

import java.util.List;

public record Round(
    int roundNumber,
    int maxAttempts,
    List<Guess> guesses,
    RoundPlayerStatus playerStatus,
    RoundStatus roundStatus,
    String solution
) {

}
