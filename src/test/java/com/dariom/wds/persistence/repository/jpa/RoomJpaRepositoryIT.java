package com.dariom.wds.persistence.repository.jpa;

import static com.dariom.wds.domain.Language.IT;
import static com.dariom.wds.domain.RoomStatus.WAITING_FOR_PLAYERS;
import static org.assertj.core.api.Assertions.assertThat;

import com.dariom.wds.domain.LetterStatus;
import com.dariom.wds.domain.RoundPlayerStatus;
import com.dariom.wds.domain.RoundStatus;
import com.dariom.wds.persistence.entity.GuessEntity;
import com.dariom.wds.persistence.entity.LetterResultEmbeddable;
import com.dariom.wds.persistence.entity.RoomEntity;
import com.dariom.wds.persistence.entity.RoundEntity;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
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
  void deleteInactive_oldRoomExists_deletesOnlyOldRooms() {
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
    var deleted = repository.deleteInactive(cutoff);

    // Assert
    assertThat(deleted).isEqualTo(1);
    assertThat(repository.findById("room-old")).isEmpty();
    assertThat(repository.findById("room-new")).isPresent();
  }

  @Test
  void deleteById_roomWithChildren_deletesChildrenAsWell() {
    // Arrange
    var roomId = "room-cascade";

    var room = new RoomEntity();
    room.setId(roomId);
    room.setLanguage(IT);
    room.setStatus(WAITING_FOR_PLAYERS);
    room.addPlayer("p1");
    room.setPlayerScore("p1", 0);

    var round = new RoundEntity();
    round.setRoom(room);
    round.setRoundNumber(1);
    round.setTargetWord("PIZZA");
    round.setMaxAttempts(6);
    round.setRoundStatus(RoundStatus.PLAYING);
    round.setPlayerStatus("p1", RoundPlayerStatus.PLAYING);

    var guess = new GuessEntity();
    guess.setRound(round);
    guess.setPlayerId("p1");
    guess.setWord("PIZZA");
    guess.setAttemptNumber(1);
    guess.setCreatedAt(Instant.parse("2025-01-01T00:00:00Z"));
    guess.setLetters(List.of(new LetterResultEmbeddable('P', LetterStatus.CORRECT)));

    round.addGuess(guess);
    room.addRound(round);

    repository.save(room);
    entityManager.flush();
    entityManager.clear();

    assertThat(count("select count(*) from rooms where id = ?", roomId)).isEqualTo(1);
    assertThat(count("select count(*) from room_players where room_id = ?", roomId)).isEqualTo(1);
    assertThat(count("select count(*) from rounds where room_id = ?", roomId)).isEqualTo(1);
    assertThat(count("select count(*) from guesses")).isEqualTo(1);
    assertThat(count("select count(*) from round_player_status")).isEqualTo(1);
    assertThat(count("select count(*) from guess_letters")).isEqualTo(1);

    // Act
    repository.deleteById(roomId);
    entityManager.flush();
    entityManager.clear();

    // Assert
    assertThat(count("select count(*) from rooms where id = ?", roomId)).isEqualTo(0);
    assertThat(count("select count(*) from room_players where room_id = ?", roomId)).isEqualTo(0);
    assertThat(count("select count(*) from rounds where room_id = ?", roomId)).isEqualTo(0);
    assertThat(count("select count(*) from guesses")).isEqualTo(0);
    assertThat(count("select count(*) from round_player_status")).isEqualTo(0);
    assertThat(count("select count(*) from guess_letters")).isEqualTo(0);
  }

  private long count(String sql, Object... params) {
    var query = entityManager.createNativeQuery(sql);
    for (var i = 0; i < params.length; i++) {
      query.setParameter(i + 1, params[i]);
    }
    return ((Number) query.getSingleResult()).longValue();
  }
}

