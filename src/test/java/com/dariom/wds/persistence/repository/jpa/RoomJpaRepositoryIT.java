package com.dariom.wds.persistence.repository.jpa;

import static com.dariom.wds.domain.Language.IT;
import static com.dariom.wds.domain.RoomStatus.WAITING_FOR_PLAYERS;
import static org.assertj.core.api.Assertions.assertThat;

import com.dariom.wds.persistence.entity.RoomEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@JpaRepositoryIT
class RoomJpaRepositoryIT {

  @Autowired
  private RoomJpaRepository repository;

  @Test
  void findWithPlayersAndScoresById_roomWithPlayersAndScores_returnsLoadedElementCollections() {
    // Arrange
    var room = new RoomEntity();
    room.setId("room-1");
    room.setLanguage(IT);
    room.setStatus(WAITING_FOR_PLAYERS);

    room.addPlayer("p2");
    room.addPlayer("p1");

    room.setPlayerScore("p1", 0);
    room.setPlayerScore("p2", 1);

    repository.save(room);

    // Act
    var found = repository.findWithPlayersAndScoresById("room-1").orElseThrow();

    // Assert
    assertThat(found.getSortedPlayerIds()).containsExactly("p1", "p2");

    var scores = found.getScoresByPlayerId();
    assertThat(scores).containsEntry("p1", 0).containsEntry("p2", 1);
    assertThat(scores).hasSize(2);
  }
}
