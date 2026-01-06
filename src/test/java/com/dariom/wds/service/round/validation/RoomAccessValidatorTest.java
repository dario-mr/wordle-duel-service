package com.dariom.wds.service.round.validation;

import static com.dariom.wds.api.v1.error.ErrorCode.ROOM_NOT_IN_PROGRESS;
import static com.dariom.wds.domain.Language.IT;
import static com.dariom.wds.domain.RoomStatus.IN_PROGRESS;
import static com.dariom.wds.domain.RoomStatus.WAITING_FOR_PLAYERS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.dariom.wds.domain.Player;
import com.dariom.wds.domain.Room;
import com.dariom.wds.exception.InvalidGuessException;
import com.dariom.wds.exception.PlayerNotInRoomException;
import java.util.List;
import org.junit.jupiter.api.Test;

class RoomAccessValidatorTest {

  @Test
  void validateRoomStatus_roomNotInProgress_throwsInvalidGuessException() {
    // Arrange
    var room = new Room(
        "room-1",
        IT,
        WAITING_FOR_PLAYERS,
        List.of(new Player("p1", 0)),
        null
    );

    // Act
    var thrown = catchThrowable(() -> RoomAccessValidator.validateRoomStatus("p1", room));

    // Assert
    assertThat(thrown)
        .isInstanceOfSatisfying(
            InvalidGuessException.class,
            ex -> assertThat(ex.getCode()).isEqualTo(ROOM_NOT_IN_PROGRESS));
  }

  @Test
  void validateRoomStatus_playerNotInRoom_throwsPlayerNotInRoomException() {
    // Arrange
    var room = new Room(
        "room-1",
        IT,
        IN_PROGRESS,
        List.of(new Player("p1", 0)),
        null
    );

    // Act
    var thrown = catchThrowable(() -> RoomAccessValidator.validateRoomStatus("p2", room));

    // Assert
    assertThat(thrown)
        .isInstanceOf(PlayerNotInRoomException.class)
        .hasMessageContaining("p2")
        .hasMessageContaining("room-1");
  }

  @Test
  void validateRoomStatus_roomInProgressAndPlayerInRoom_doesNotThrow() {
    // Arrange
    var room = new Room(
        "room-1",
        IT,
        IN_PROGRESS,
        List.of(new Player("p1", 0)),
        null
    );

    // Act
    RoomAccessValidator.validateRoomStatus("p1", room);
  }
}
