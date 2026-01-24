package com.dariom.wds.persistence.repository.jpa;

import static com.dariom.wds.domain.Language.IT;
import static com.dariom.wds.domain.RoomStatus.WAITING_FOR_PLAYERS;
import static org.assertj.core.api.Assertions.assertThat;

import com.dariom.wds.persistence.entity.RoomEntity;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@JpaRepositoryIT
class RoomJpaRepositoryIT {

  @Autowired
  private RoomJpaRepository repository;

  @Autowired
  private EntityManager entityManager;

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

  @Test
  void deleteByLastUpdatedAtBefore_oldRoomExists_deletesOnlyOldRooms() {
    // Arrange
    var oldRoom = new RoomEntity();
    oldRoom.setId("room-old");
    oldRoom.setLanguage(IT);
    oldRoom.setStatus(WAITING_FOR_PLAYERS);

    var newRoom = new RoomEntity();
    newRoom.setId("room-new");
    newRoom.setLanguage(IT);
    newRoom.setStatus(WAITING_FOR_PLAYERS);

    repository.save(oldRoom);
    repository.save(newRoom);
    entityManager.flush();

    var oldTs = Instant.parse("2020-01-01T00:00:00Z");
    var newTs = Instant.parse("2025-01-01T00:00:00Z");

    entityManager.createNativeQuery("update rooms set last_updated_at = :ts where id = :id")
        .setParameter("ts", oldTs)
        .setParameter("id", "room-old")
        .executeUpdate();

    entityManager.createNativeQuery("update rooms set last_updated_at = :ts where id = :id")
        .setParameter("ts", newTs)
        .setParameter("id", "room-new")
        .executeUpdate();

    entityManager.flush();
    entityManager.clear();

    var cutoff = Instant.parse("2024-01-01T00:00:00Z");

    // Act
    var deleted = repository.deleteByLastUpdatedAtBefore(cutoff);

    // Assert
    assertThat(deleted).isEqualTo(1);
    assertThat(repository.findById("room-old")).isEmpty();
    assertThat(repository.findById("room-new")).isPresent();
  }
}
