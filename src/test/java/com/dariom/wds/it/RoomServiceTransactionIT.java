package com.dariom.wds.it;

import static com.dariom.wds.domain.Language.IT;
import static com.dariom.wds.domain.RoomStatus.WAITING_FOR_PLAYERS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.dariom.wds.persistence.entity.RoomEntity;
import com.dariom.wds.persistence.repository.jpa.RoomJpaRepository;
import com.dariom.wds.service.room.RoomService;
import com.dariom.wds.service.round.RoundService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class RoomServiceTransactionIT {

  private static final String ROOM_ID = "room-1";
  private static final String PLAYER_1 = "p1";
  private static final String PLAYER_2 = "p2";

  @Autowired
  private RoomService roomService;

  @Autowired
  private RoomJpaRepository roomJpaRepository;

  @MockitoBean
  private RoundService roundService;

  @Test
  void joinRoom_exceptionAfterSave_rollsBackDatabaseChanges() {
    // Arrange
    var room = new RoomEntity();
    room.setId(ROOM_ID);
    room.setLanguage(IT);
    room.setStatus(WAITING_FOR_PLAYERS);
    room.addPlayer(PLAYER_1);
    room.setPlayerScore(PLAYER_1, 0);
    // Prevent joinRoom from starting a new round (so we can fail after save).
    room.setCurrentRoundNumber(1);
    roomJpaRepository.save(room);

    when(roundService.getCurrentRound(eq(ROOM_ID), anyInt()))
        .thenThrow(new RuntimeException("boom"));

    // Act
    assertThatThrownBy(() -> roomService.joinRoom(ROOM_ID, PLAYER_2))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("boom");

    // Assert
    var reloadedRoom = roomJpaRepository.findWithPlayersAndScoresById(ROOM_ID)
        .orElseThrow();

    assertThat(reloadedRoom.getStatus()).isEqualTo(WAITING_FOR_PLAYERS);
    assertThat(reloadedRoom.getCurrentRoundNumber()).isEqualTo(1);
    assertThat(reloadedRoom.getPlayerIds()).containsExactlyInAnyOrder(PLAYER_1);
    assertThat(reloadedRoom.getScoresByPlayerId())
        .containsExactlyInAnyOrderEntriesOf(Map.of(PLAYER_1, 0));
  }
}
