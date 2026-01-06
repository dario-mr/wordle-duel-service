package com.dariom.wds.exception;

public class RoomClosedException extends RuntimeException {

  public RoomClosedException(String roomId) {
    super("Room <%s> is closed".formatted(roomId));
  }
}
