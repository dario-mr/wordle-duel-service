package com.dariom.wds.service.round.validation;

import static com.dariom.wds.domain.RoomStatus.IN_PROGRESS;

import com.dariom.wds.domain.RoomStatus;
import com.dariom.wds.exception.PlayerNotInRoomException;
import com.dariom.wds.exception.RoomNotReadyException;
import java.util.Set;

public class RoomAccessValidator {

  private RoomAccessValidator() {
  }

  public static void validateRoomStatus(String playerId, String roomId, RoomStatus status,
      Set<String> roomPlayerIds) {
    if (status != IN_PROGRESS) {
      throw new RoomNotReadyException(roomId, status, IN_PROGRESS);
    }

    if (!roomPlayerIds.contains(playerId)) {
      throw new PlayerNotInRoomException(playerId, roomId);
    }
  }
}
