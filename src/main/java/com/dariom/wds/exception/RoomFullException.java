package com.dariom.wds.exception;

public class RoomFullException extends RuntimeException {

  public RoomFullException(String roomId) {
    super("Room <%s> is full".formatted(roomId));
  }
}
