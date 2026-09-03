package com.dariom.wds.exception;

public class RoomMessageLimitReachedException extends RuntimeException {

  public RoomMessageLimitReachedException(String playerId, String roomId) {
    super("Player <%s> has reached the message limit in room <%s>"
        .formatted(playerId, roomId));
  }
}
