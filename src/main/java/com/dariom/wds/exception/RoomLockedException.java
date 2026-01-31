package com.dariom.wds.exception;

public class RoomLockedException extends RuntimeException {

  public RoomLockedException(String roomId) {
    super("Room <%s> is busy".formatted(roomId));
  }
}
