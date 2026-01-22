package com.dariom.wds.exception;

public class RoomAccessDeniedException extends RuntimeException {

  public RoomAccessDeniedException(String roomId, String playerId) {
    super("Player <%s> cannot inspect room <%s>".formatted(playerId, roomId));
  }
}
