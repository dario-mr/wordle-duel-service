package com.dariom.wds.exception;

public class RoomNotReadyException extends RuntimeException {

  public RoomNotReadyException(String roomId, int playerCount) {
    super("Room <%s> is not ready: expected 2 players, got %d".formatted(roomId, playerCount));
  }
}
