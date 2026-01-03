package com.dariom.wds.service.round.validation;

import static com.dariom.wds.api.v1.error.ErrorCode.PLAYER_DONE;
import static com.dariom.wds.domain.RoundPlayerStatus.PLAYING;

import com.dariom.wds.domain.RoundPlayerStatus;
import com.dariom.wds.exception.InvalidGuessException;
import com.dariom.wds.exception.PlayerNotInRoomException;

public class PlayerStatusValidator {

  private PlayerStatusValidator() {
  }

  public static void validatePlayerStatus(
      String roomId,
      String playerId,
      RoundPlayerStatus playerStatus) {
    if (playerStatus == null) {
      throw new PlayerNotInRoomException(playerId, roomId);
    }

    if (playerStatus != PLAYING) {
      throw new InvalidGuessException(PLAYER_DONE, "Player already finished this round");
    }
  }

}
