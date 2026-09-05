package com.dariom.wds.service.room;

import com.dariom.wds.domain.Room;
import com.dariom.wds.exception.RoomFullException;

public class RoomValidator {

  private RoomValidator() {
  }

  public static void validateRoom(String joiningPlayerId, Room room, int maxPlayers) {
    validateRoomNotFull(room, joiningPlayerId, maxPlayers);
  }

  private static void validateRoomNotFull(Room room, String joiningPlayerId, int maxPlayers) {
    if (!room.hasPlayer(joiningPlayerId) && room.players().size() >= maxPlayers) {
      throw new RoomFullException(room.id());
    }
  }

}
