package com.dariom.wds.exception;

public class RoomNotFoundException extends RuntimeException {

  public RoomNotFoundException(String roomId) {
    super("Room <%s> not found".formatted(roomId));
  }
}
