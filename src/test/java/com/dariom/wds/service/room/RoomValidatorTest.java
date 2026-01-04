package com.dariom.wds.service.room;

import static com.dariom.wds.domain.Language.IT;
import static com.dariom.wds.domain.RoomStatus.CLOSED;
import static com.dariom.wds.domain.RoomStatus.WAITING_FOR_PLAYERS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.dariom.wds.domain.Room;
import com.dariom.wds.exception.RoomClosedException;
import com.dariom.wds.exception.RoomFullException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RoomValidatorTest {

  @Test
  void validateRoom_roomClosed_throwsRoomClosedException() {
    // Arrange
    var room = new Room("room-1", IT, CLOSED, List.of("p1"), Map.of("p1", 0), null);

    // Act
    var thrown = catchThrowable(() -> RoomValidator.validateRoom("p2", room, 2));

    // Assert
    assertThat(thrown)
        .isInstanceOf(RoomClosedException.class)
        .hasMessageContaining("room-1");
  }

  @Test
  void validateRoom_roomFullAndPlayerNotInRoom_throwsRoomFullException() {
    // Arrange
    var room = new Room(
        "room-1",
        IT,
        WAITING_FOR_PLAYERS,
        List.of("p1", "p2"),
        Map.of("p1", 0, "p2", 0),
        null
    );

    // Act
    var thrown = catchThrowable(() -> RoomValidator.validateRoom("p3", room, 2));

    // Assert
    assertThat(thrown)
        .isInstanceOf(RoomFullException.class)
        .hasMessageContaining("room-1");
  }

  @Test
  void validateRoom_roomFullButPlayerAlreadyInRoom_doesNotThrow() {
    // Arrange
    var room = new Room(
        "room-1",
        IT,
        WAITING_FOR_PLAYERS,
        List.of("p1", "p2"),
        Map.of("p1", 0, "p2", 0),
        null
    );

    // Act
    RoomValidator.validateRoom("p2", room, 2);
  }
}
