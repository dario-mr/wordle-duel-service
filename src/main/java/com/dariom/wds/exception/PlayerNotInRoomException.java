package com.dariom.wds.exception;

public class PlayerNotInRoomException extends RuntimeException {

  public PlayerNotInRoomException(String playerId, String roomId) {
    super("Player %s is not in room %s".formatted(playerId, roomId));
  }
}
