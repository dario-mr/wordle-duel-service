package com.dariom.wds.service.round.validation;

import static com.dariom.wds.api.common.ErrorCode.PLAYER_DONE;
import static com.dariom.wds.domain.RoundPlayerStatus.LOST;
import static com.dariom.wds.domain.RoundPlayerStatus.PLAYING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.dariom.wds.exception.InvalidGuessException;
import com.dariom.wds.exception.PlayerNotInRoomException;
import org.junit.jupiter.api.Test;

class PlayerStatusValidatorTest {

  @Test
  void validatePlayerStatus_playerStatusNull_throwsPlayerNotInRoomException() {
    // Arrange
    var roomId = "room-1";
    var playerId = "p1";

    // Act
    var thrown = catchThrowable(
        () -> PlayerStatusValidator.validatePlayerStatus(roomId, playerId, null));

    // Assert
    assertThat(thrown)
        .isInstanceOf(PlayerNotInRoomException.class)
        .hasMessageContaining(playerId)
        .hasMessageContaining(roomId);
  }

  @Test
  void validatePlayerStatus_playerAlreadyFinished_throwsInvalidGuessException() {
    // Arrange
    var roomId = "room-1";
    var playerId = "p1";

    // Act
    var thrown = catchThrowable(
        () -> PlayerStatusValidator.validatePlayerStatus(roomId, playerId, LOST));

    // Assert
    assertThat(thrown)
        .isInstanceOfSatisfying(
            InvalidGuessException.class,
            ex -> assertThat(ex.getCode()).isEqualTo(PLAYER_DONE));
  }

  @Test
  void validatePlayerStatus_playerIsPlaying_doesNotThrow() {
    // Arrange
    var roomId = "room-1";
    var playerId = "p1";

    // Act
    PlayerStatusValidator.validatePlayerStatus(roomId, playerId, PLAYING);
  }
}
