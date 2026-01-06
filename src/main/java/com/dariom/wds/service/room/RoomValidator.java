package com.dariom.wds.service.room;

import static com.dariom.wds.domain.RoomStatus.CLOSED;

import java.util.Optional;

import com.dariom.wds.domain.Room;
import com.dariom.wds.exception.RoomClosedException;
import com.dariom.wds.exception.RoomFullException;

public class RoomValidator {

  private RoomValidator() {
  }

  public static void validateRoom(String playerId, Room room, int maxPlayers) {
    validateRoomNotClosed(room);
    validateRoomNotFull(room, playerId, maxPlayers);
  }

  public static Optional<String> validateRoomAsMessage(String playerId, Room room, int maxPlayers) {
    if (room == null) {
      return Optional.of("room is null");
    }

    if (isClosed(room)) {
      return Optional.of("room is closed");
    }

    if (!room.hasPlayer(playerId) && wouldBeFull(room, maxPlayers)) {
      return Optional.of("room is full");
    }

    return Optional.empty();
  }

  private static void validateRoomNotClosed(Room room) {
    if (room.status() == CLOSED) {
      throw new RoomClosedException(room.id());
    }
  }

  private static void validateRoomNotFull(Room room, String playerId, int maxPlayers) {
    if (!room.hasPlayer(playerId) && room.players().size() >= maxPlayers) {
      throw new RoomFullException(room.id());
    }
  }

  public static boolean isClosed(Room room) {
    return room.status() == CLOSED;
  }

  public static boolean wouldBeFull(Room room, int maxPlayers) {
    if (maxPlayers <= 0) {
      return true;
    }

    return room.players().size() >= maxPlayers;
  }

}
