package com.dariom.wds.persistence.repository;

import static com.dariom.wds.domain.Language.IT;
import static com.dariom.wds.domain.LetterStatus.ABSENT;
import static com.dariom.wds.domain.LetterStatus.CORRECT;
import static com.dariom.wds.domain.LetterStatus.PRESENT;
import static com.dariom.wds.domain.RoomStatus.IN_PROGRESS;
import static com.dariom.wds.domain.RoundPlayerStatus.LOST;
import static com.dariom.wds.domain.RoundPlayerStatus.WON;
import static com.dariom.wds.domain.RoundStatus.ENDED;
import static com.dariom.wds.domain.RoundStatus.PLAYING;
import static org.assertj.core.api.Assertions.assertThat;

import com.dariom.wds.domain.RoundPlayerStatus;
import com.dariom.wds.persistence.entity.GuessEntity;
import com.dariom.wds.persistence.entity.LetterResultEmbeddable;
import com.dariom.wds.persistence.entity.RoomEntity;
import com.dariom.wds.persistence.entity.RoundEntity;
import com.dariom.wds.persistence.repository.jpa.JpaRepositoryIT;
import com.dariom.wds.persistence.repository.jpa.RoomJpaRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@JpaRepositoryIT
@Import(RoundReadRepository.class)
class RoundReadRepositoryIT {

  @Autowired
  private RoundReadRepository roundReadRepository;

  @Autowired
  private RoomJpaRepository roomJpaRepository;

  @Autowired
  private EntityManager entityManager;

  @Test
  void findByRoomIdAndRoundNumber_existingRound_returnsAssembledRound() {
    // Arrange
    var room = room("room-1");
    var round = round(room, 1, PLAYING);
    round.setPlayerStatus("p1", RoundPlayerStatus.PLAYING);
    round.setPlayerStatus("p2", LOST);
    round.addGuess(guess(round, "p1", "PIZZA", 1, List.of(
        new LetterResultEmbeddable('P', CORRECT),
        new LetterResultEmbeddable('I', PRESENT)
    )));
    round.addGuess(guess(round, "p2", "PASTA", 1, List.of(
        new LetterResultEmbeddable('P', CORRECT),
        new LetterResultEmbeddable('A', ABSENT)
    )));
    room.addRound(round);
    room.setCurrentRoundNumber(1);

    roomJpaRepository.save(room);
    entityManager.flush();
    entityManager.clear();

    // Act
    var found = roundReadRepository.findByRoomIdAndRoundNumber("room-1", 1).orElseThrow();

    // Assert
    assertThat(found.roundNumber()).isEqualTo(1);
    assertThat(found.maxAttempts()).isEqualTo(6);
    assertThat(found.roundStatus()).isEqualTo(PLAYING);
    assertThat(found.solution()).isEqualTo("PIZZA");
    assertThat(found.statusByPlayerId())
        .containsEntry("p1", RoundPlayerStatus.PLAYING)
        .containsEntry("p2", LOST);
    assertThat(found.guessesByPlayerId()).containsOnlyKeys("p1", "p2");
    assertThat(found.guessesByPlayerId().get("p1")).hasSize(1);
    assertThat(found.guessesByPlayerId().get("p1").getFirst().letters())
        .extracting(letter -> letter.status().name())
        .containsExactly("CORRECT", "PRESENT");
    assertThat(found.guessesByPlayerId().get("p2").getFirst().letters())
        .extracting(letter -> letter.status().name())
        .containsExactly("CORRECT", "ABSENT");
  }

  @Test
  void findCurrentByRoomIds_roomsHaveCurrentRounds_returnsMapByRoomId() {
    // Arrange
    var room1 = room("room-1");
    var room1Round = round(room1, 1, PLAYING);
    room1Round.setPlayerStatus("p1", RoundPlayerStatus.PLAYING);
    room1Round.addGuess(guess(room1Round, "p1", "PIZZA", 1,
        List.of(new LetterResultEmbeddable('P', CORRECT))));
    room1.addRound(room1Round);
    room1.setCurrentRoundNumber(1);

    var room2 = room("room-2");
    var oldRound = round(room2, 1, ENDED);
    oldRound.setPlayerStatus("p1", WON);
    var currentRound = round(room2, 2, PLAYING);
    currentRound.setPlayerStatus("p1", RoundPlayerStatus.PLAYING);
    currentRound.addGuess(guess(currentRound, "p1", "PASTA", 1,
        List.of(new LetterResultEmbeddable('P', PRESENT))));
    room2.addRound(oldRound);
    room2.addRound(currentRound);
    room2.setCurrentRoundNumber(2);

    var roomWithoutCurrent = room("room-3");
    var roundWithoutCurrent = round(roomWithoutCurrent, 1, PLAYING);
    roomWithoutCurrent.addRound(roundWithoutCurrent);

    roomJpaRepository.save(room1);
    roomJpaRepository.save(room2);
    roomJpaRepository.save(roomWithoutCurrent);
    entityManager.flush();
    entityManager.clear();

    // Act
    var found = roundReadRepository.findCurrentByRoomIds(List.of("room-1", "room-2", "room-3"));

    // Assert
    assertThat(found).containsOnlyKeys("room-1", "room-2");
    assertThat(found.get("room-1").roundNumber()).isEqualTo(1);
    assertThat(found.get("room-2").roundNumber()).isEqualTo(2);
    assertThat(found.get("room-2").guessesByPlayerId().get("p1").getFirst().word())
        .isEqualTo("PASTA");
  }

  private static RoomEntity room(String roomId) {
    var room = new RoomEntity();
    room.setId(roomId);
    room.setLanguage(IT);
    room.setStatus(IN_PROGRESS);
    room.addPlayer("p1");
    room.addPlayer("p2");
    room.setPlayerScore("p1", 0);
    room.setPlayerScore("p2", 0);
    return room;
  }

  private static RoundEntity round(RoomEntity room, int roundNumber,
      com.dariom.wds.domain.RoundStatus roundStatus) {
    var round = new RoundEntity();
    round.setRoom(room);
    round.setRoundNumber(roundNumber);
    round.setTargetWord("PIZZA");
    round.setMaxAttempts(6);
    round.setRoundStatus(roundStatus);
    return round;
  }

  private static GuessEntity guess(RoundEntity round, String playerId, String word,
      int attemptNumber, List<LetterResultEmbeddable> letters) {
    var guess = new GuessEntity();
    guess.setRound(round);
    guess.setPlayerId(playerId);
    guess.setWord(word);
    guess.setAttemptNumber(attemptNumber);
    guess.setLetters(letters);
    return guess;
  }
}
