package com.dariom.wds.domain;

import java.util.List;

public record Room(
    String id,
    Language language,
    RoomStatus status,
    List<Player> players,
    Round currentRound
) {

  public boolean hasPlayer(String playerId) {
    return players.stream()
        .anyMatch(p -> p.id().equals(playerId));
  }
}
