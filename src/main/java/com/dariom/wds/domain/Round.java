package com.dariom.wds.domain;

import java.util.List;
import java.util.Map;

public record Round(
    int roundNumber,
    int maxAttempts,
    Map<String, List<Guess>> guessesByPlayerId,
    Map<String, RoundPlayerStatus> statusByPlayerId,
    RoundStatus roundStatus,
    String solution
) {

}
