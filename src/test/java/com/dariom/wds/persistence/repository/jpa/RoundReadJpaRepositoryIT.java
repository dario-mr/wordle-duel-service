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
  void findFlatRowsByRoomIdAndRoundNumber_existingRound_returnsProjectionRows() {
    // Arrange
    saveRoundGraph("room-1", 1);

    // Act
    var found = roundReadJpaRepository.findFlatRowsByRoomIdAndRoundNumber("room-1", 1);

    // Assert
    assertThat(found).isNotEmpty();
    assertThat(found)
        .extracting(row -> row.roomId() + ":" + row.roundNumber() + ":" + row.targetWord())
        .containsOnly("room-1:1:PIZZA");
    assertThat(found)
        .extracting(row -> row.statusPlayerId() + ":" + row.playerStatus().name())
        .contains("p1:PLAYING", "p2:LOST");
    assertThat(found)
        .filteredOn(row -> row.guessId() >= 0)
        .extracting(row -> "%s:%d:%s".formatted(
            row.guessPlayerId(),
            row.attemptNumber(),
            row.letterStatus().name()
        ))
        .contains("p1:1:CORRECT", "p1:1:PRESENT");
  }

  @Test
  void findCurrentFlatRowsByRoomIds_roomsHaveCurrentRounds_returnsProjectionRowsForCurrentRounds() {
    // Arrange
    saveRoundGraph("room-1", 1);
    saveRoundGraph("room-2", 1, ENDED, false);
    saveRoundGraph("room-2", 2);
    saveRoomWithoutCurrentRound("room-3");

    // Act
    var found = roundReadJpaRepository.findCurrentFlatRowsByRoomIds(List.of(
        "room-1",
        "room-2",
        "room-3"
    ));

    // Assert
    assertThat(found)
        .extracting(row -> "%s:%d".formatted(row.roomId(), row.roundNumber()))
        .contains("room-1:1", "room-2:2");
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
