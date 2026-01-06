package com.dariom.wds.service.round.validation;

import static com.dariom.wds.api.v1.error.ErrorCode.ROOM_NOT_IN_PROGRESS;
import static com.dariom.wds.domain.RoomStatus.IN_PROGRESS;

import com.dariom.wds.domain.Room;
import com.dariom.wds.exception.InvalidGuessException;
import com.dariom.wds.exception.PlayerNotInRoomException;

public class RoomAccessValidator {

  private RoomAccessValidator() {
  }

  public static void validateRoomStatus(String playerId, Room room) {
    if (room.status() != IN_PROGRESS) {
      throw new InvalidGuessException(ROOM_NOT_IN_PROGRESS, "Room is not in progress");
    }

    if (!room.hasPlayer(playerId)) {
      throw new PlayerNotInRoomException(playerId, room.id());
    }
  }
}
