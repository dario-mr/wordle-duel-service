package com.dariom.wds.service.room;

import static com.dariom.wds.domain.RoomStatus.CLOSED;

import com.dariom.wds.domain.Room;
import com.dariom.wds.exception.RoomClosedException;
import com.dariom.wds.exception.RoomFullException;

public class RoomValidator {

  private RoomValidator() {
  }

  public static void validateRoom(String joiningPlayerId, Room room, int maxPlayers) {
    validateRoomNotClosed(room);
    validateRoomNotFull(room, joiningPlayerId, maxPlayers);
  }

  private static void validateRoomNotClosed(Room room) {
    if (room.status() == CLOSED) {
      throw new RoomClosedException(room.id());
    }
  }

  private static void validateRoomNotFull(Room room, String joiningPlayerId, int maxPlayers) {
    if (!room.hasPlayer(joiningPlayerId) && room.players().size() >= maxPlayers) {
      throw new RoomFullException(room.id());
    }
  }

}
