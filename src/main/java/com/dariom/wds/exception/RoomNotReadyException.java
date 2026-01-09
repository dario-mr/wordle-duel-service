package com.dariom.wds.exception;

import com.dariom.wds.domain.RoomStatus;

public class RoomNotReadyException extends RuntimeException {

  public RoomNotReadyException(String roomId, int playerCount) {
    super("Room <%s> is not ready: expected 2 players, got %d".formatted(roomId, playerCount));
  }

  public RoomNotReadyException(String roomId, RoomStatus currentStatus, RoomStatus requiredStatus) {
    super("Room <%s> is not ready: required status %s, got %s".formatted(
        roomId, requiredStatus, currentStatus));
  }
}
