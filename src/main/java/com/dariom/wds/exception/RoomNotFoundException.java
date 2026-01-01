package com.dariom.wds.exception;

public class RoomNotFoundException extends RuntimeException {

  public RoomNotFoundException(String roomId) {
    super("Room not found: %s".formatted(roomId));
  }
}
