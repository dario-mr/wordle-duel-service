package com.dariom.wds.persistence.repository.jpa;

import static com.dariom.wds.domain.Language.IT;
import static com.dariom.wds.domain.LetterStatus.CORRECT;
import static com.dariom.wds.domain.LetterStatus.PRESENT;
import static com.dariom.wds.domain.RoomStatus.IN_PROGRESS;
import static com.dariom.wds.domain.RoundStatus.ENDED;
import static com.dariom.wds.domain.RoundStatus.PLAYING;
import static org.assertj.core.api.Assertions.assertThat;

import com.dariom.wds.domain.RoundPlayerStatus;
import com.dariom.wds.domain.RoundStatus;
import com.dariom.wds.persistence.entity.GuessEntity;
import com.dariom.wds.persistence.entity.LetterResultEmbeddable;
import com.dariom.wds.persistence.entity.RoomEntity;
import com.dariom.wds.persistence.entity.RoundEntity;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@JpaRepositoryIT
class RoundReadJpaRepositoryIT {

  @Autowired
  private RoundReadJpaRepository roundReadJpaRepository;

  @Autowired
  private RoomJpaRepository roomJpaRepository;

  @Test
  void findRoundHeaderByRoomIdAndRoundNumber_existingRound_returnsProjection() {
    // Arrange
    saveRoundGraph("room-1", 1);

    // Act
    var found = roundReadJpaRepository.findRoundHeaderByRoomIdAndRoundNumber("room-1", 1)
        .orElseThrow();

    // Assert
    assertThat(found.roundId()).isNotNull();
    assertThat(found.roomId()).isEqualTo("room-1");
    assertThat(found.roundNumber()).isEqualTo(1);
    assertThat(found.maxAttempts()).isEqualTo(6);
    assertThat(found.roundStatus()).isEqualTo(PLAYING);
    assertThat(found.targetWord()).isEqualTo("PIZZA");
  }

  @Test
  void findCurrentRoundHeadersByRoomIds_roomsHaveCurrentRounds_returnsCurrentRoundProjections() {
    // Arrange
    saveRoundGraph("room-1", 1);
    saveRoundGraph("room-2", 1, ENDED, false);
    saveRoundGraph("room-2", 2);
    saveRoomWithoutCurrentRound("room-3");

    // Act
    var found = roundReadJpaRepository.findCurrentRoundHeadersByRoomIds(List.of(
        "room-1",
        "room-2",
        "room-3"
    ));

    // Assert
    assertThat(found)
        .extracting(header -> "%s:%d".formatted(header.roomId(), header.roundNumber()))
        .containsExactlyInAnyOrder("room-1:1", "room-2:2");
  }

  @Test
  void findRoundStatusesByRoundIds_existingRound_returnsStatusProjections() {
    // Arrange
    saveRoundGraph("room-1", 1);
    var roundId = roundReadJpaRepository.findRoundHeaderByRoomIdAndRoundNumber("room-1", 1)
        .orElseThrow()
        .roundId();

    // Act
    var found = roundReadJpaRepository.findRoundStatusesByRoundIds(List.of(roundId));

    // Assert
    assertThat(found)
        .extracting(status -> status.playerId() + ":" + status.status().name())
        .containsExactlyInAnyOrder("p1:PLAYING", "p2:LOST");
  }

  @Test
  void findGuessLettersByRoundIds_existingRound_returnsGuessLetterProjections() {
    // Arrange
    saveRoundGraph("room-1", 1);
    var roundId = roundReadJpaRepository.findRoundHeaderByRoomIdAndRoundNumber("room-1", 1)
        .orElseThrow()
        .roundId();

    // Act
    var found = roundReadJpaRepository.findGuessLettersByRoundIds(List.of(roundId));

    // Assert
    assertThat(found)
        .extracting(letter -> "%s:%d:%s".formatted(
            letter.playerId(),
            letter.attemptNumber(),
            letter.letterStatus().name()
        ))
        .containsExactly("p1:1:CORRECT", "p1:1:PRESENT");
  }

  private void saveRoundGraph(String roomId, int roundNumber) {
    saveRoundGraph(roomId, roundNumber, PLAYING, true);
  }

  private void saveRoundGraph(
      String roomId,
      int roundNumber,
      RoundStatus roundStatus,
      boolean currentRound
  ) {
    var room = new RoomEntity();
    room.setId(roomId);
    room.setLanguage(IT);
    room.setStatus(IN_PROGRESS);
    room.addPlayer("p1");
    room.addPlayer("p2");
    room.setPlayerScore("p1", 0);
    room.setPlayerScore("p2", 0);

    var round = new RoundEntity();
    round.setRoom(room);
    round.setRoundNumber(roundNumber);
    round.setTargetWord("PIZZA");
    round.setMaxAttempts(6);
    round.setRoundStatus(roundStatus);
    round.setPlayerStatus("p1", RoundPlayerStatus.PLAYING);
    round.setPlayerStatus("p2", RoundPlayerStatus.LOST);
    room.addRound(round);
    if (currentRound) {
      room.setCurrentRoundNumber(roundNumber);
    }

    var guess = new GuessEntity();
    guess.setRound(round);
    guess.setPlayerId("p1");
    guess.setWord("PIZZA");
    guess.setAttemptNumber(1);
    guess.setLetters(List.of(
        new LetterResultEmbeddable('P', CORRECT),
        new LetterResultEmbeddable('I', PRESENT)
    ));
    round.addGuess(guess);

    roomJpaRepository.save(room);
    roomJpaRepository.flush();
  }

  private void saveRoomWithoutCurrentRound(String roomId) {
    var room = new RoomEntity();
    room.setId(roomId);
    room.setLanguage(IT);
    room.setStatus(IN_PROGRESS);
    room.addPlayer("p1");
    room.addPlayer("p2");
    room.setPlayerScore("p1", 0);
    room.setPlayerScore("p2", 0);

    var round = new RoundEntity();
    round.setRoom(room);
    round.setRoundNumber(1);
    round.setTargetWord("PIZZA");
    round.setMaxAttempts(6);
    round.setRoundStatus(PLAYING);
    room.addRound(round);

    roomJpaRepository.save(room);
    roomJpaRepository.flush();
  }
}
