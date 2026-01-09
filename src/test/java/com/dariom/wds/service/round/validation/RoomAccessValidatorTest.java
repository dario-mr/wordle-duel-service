package com.dariom.wds.service.round.validation;

import static com.dariom.wds.domain.Language.IT;
import static com.dariom.wds.domain.RoomStatus.IN_PROGRESS;
import static com.dariom.wds.domain.RoomStatus.WAITING_FOR_PLAYERS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.dariom.wds.domain.Player;
import com.dariom.wds.domain.Room;
import com.dariom.wds.domain.RoomStatus;
import com.dariom.wds.exception.PlayerNotInRoomException;
import com.dariom.wds.exception.RoomNotReadyException;
import java.util.Arrays;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RoomAccessValidatorTest {

  @Test
  void validateRoomStatus_roomNotInProgress_throwsRoomNotReadyException() {
    // Arrange
    var room = room(WAITING_FOR_PLAYERS, "p1");

    // Act
    var thrown = catchThrowable(() -> RoomAccessValidator.validateRoomStatus(
        "p1",
        room.id(),
        room.status(),
        Set.of("p1")
    ));

    // Assert
    assertThat(thrown)
        .isInstanceOf(RoomNotReadyException.class)
        .hasMessage(
            "Room <room-1> is not ready: required status IN_PROGRESS, got WAITING_FOR_PLAYERS");
  }

  @Test
  void validateRoomStatus_playerNotInRoom_throwsPlayerNotInRoomException() {
    // Arrange
    var room = room(IN_PROGRESS, "p1");

    // Act
    var thrown = catchThrowable(() -> RoomAccessValidator.validateRoomStatus(
        "p2",
        room.id(),
        room.status(),
        Set.of("p1")
    ));

    // Assert
    assertThat(thrown)
        .isInstanceOf(PlayerNotInRoomException.class)
        .hasMessageContaining("p2")
        .hasMessageContaining("room-1");
  }

  @Test
  void validateRoomStatus_roomInProgressAndPlayerInRoom_doesNotThrow() {
    // Arrange
    var room = room(IN_PROGRESS, "p1");

    // Act
    RoomAccessValidator.validateRoomStatus(
        "p1",
        room.id(),
        room.status(),
        Set.of("p1")
    );
  }

  private static Room room(RoomStatus status, String... playerIds) {
    return new Room(
        "room-1",
        IT,
        status,
        Arrays.stream(playerIds).map(pid -> new Player(pid, 0)).toList(),
        null
    );
  }
}
