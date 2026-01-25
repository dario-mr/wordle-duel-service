package com.dariom.wds.persistence.repository.jpa;

import static com.dariom.wds.domain.Language.IT;
import static com.dariom.wds.domain.LetterStatus.CORRECT;
import static com.dariom.wds.domain.RoomStatus.IN_PROGRESS;
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
class RoundJpaRepositoryIT {

  @Autowired
  private RoundJpaRepository roundJpaRepository;

  @Autowired
  private RoomJpaRepository roomJpaRepository;

  @Test
  void findWithDetailsByRoomIdAndRoundNumber_existingRound_returnsGuessesAndLetters() {
    // Arrange
    var room = new RoomEntity();
    room.setId("room-1");
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
    round.setRoundStatus(RoundStatus.PLAYING);

    round.setPlayerStatus("p1", RoundPlayerStatus.PLAYING);
    round.setPlayerStatus("p2", RoundPlayerStatus.PLAYING);

    room.addRound(round);
    room.setCurrentRoundNumber(1);

    var guess = new GuessEntity();
    guess.setRound(round);
    guess.setPlayerId("p1");
    guess.setWord("PIZZA");
    guess.setAttemptNumber(1);
    guess.setLetters(List.of(new LetterResultEmbeddable('P', CORRECT)));

    round.addGuess(guess);

    roomJpaRepository.save(room);

    // Act
    var found = roundJpaRepository.findWithDetailsByRoomIdAndRoundNumber("room-1", 1).orElseThrow();

    // Assert
    assertThat(found.getGuesses()).hasSize(1);

    var foundGuess = found.getGuesses().getFirst();
    assertThat(foundGuess.getWord()).isEqualTo("PIZZA");

    assertThat(foundGuess.getLetters()).hasSize(1);
    assertThat(foundGuess.getLetters().getFirst().getStatus()).isEqualTo(CORRECT);
  }

  @Test
  void findCurrentRoundsWithDetailsByRoomIds_roomsHaveCurrentRounds_returnsOnlyCurrentRounds() {
    // Arrange
    var room1 = new RoomEntity();
    room1.setId("room-1");
    room1.setLanguage(IT);
    room1.setStatus(IN_PROGRESS);
    room1.addPlayer("p1");
    room1.setPlayerScore("p1", 0);

    var round1 = new RoundEntity();
    round1.setRoom(room1);
    round1.setRoundNumber(1);
    round1.setTargetWord("PIZZA");
    round1.setMaxAttempts(6);
    round1.setRoundStatus(RoundStatus.PLAYING);
    room1.addRound(round1);
    room1.setCurrentRoundNumber(1);

    var guess1 = new GuessEntity();
    guess1.setRound(round1);
    guess1.setPlayerId("p1");
    guess1.setWord("PIZZA");
    guess1.setAttemptNumber(1);
    guess1.setLetters(List.of(new LetterResultEmbeddable('P', CORRECT)));
    round1.addGuess(guess1);

    var room2 = new RoomEntity();
    room2.setId("room-2");
    room2.setLanguage(IT);
    room2.setStatus(IN_PROGRESS);
    room2.addPlayer("p1");
    room2.setPlayerScore("p1", 0);

    var oldRound = new RoundEntity();
    oldRound.setRoom(room2);
    oldRound.setRoundNumber(1);
    oldRound.setTargetWord("PIZZA");
    oldRound.setMaxAttempts(6);
    oldRound.setRoundStatus(RoundStatus.ENDED);

    var currentRound = new RoundEntity();
    currentRound.setRoom(room2);
    currentRound.setRoundNumber(2);
    currentRound.setTargetWord("PIZZA");
    currentRound.setMaxAttempts(6);
    currentRound.setRoundStatus(RoundStatus.PLAYING);

    room2.addRound(oldRound);
    room2.addRound(currentRound);
    room2.setCurrentRoundNumber(2);

    var guess2 = new GuessEntity();
    guess2.setRound(currentRound);
    guess2.setPlayerId("p1");
    guess2.setWord("PIZZA");
    guess2.setAttemptNumber(1);
    guess2.setLetters(List.of(new LetterResultEmbeddable('P', CORRECT)));
    currentRound.addGuess(guess2);

    var roomNoCurrent = new RoomEntity();
    roomNoCurrent.setId("room-no-current");
    roomNoCurrent.setLanguage(IT);
    roomNoCurrent.setStatus(IN_PROGRESS);
    roomNoCurrent.addPlayer("p1");
    roomNoCurrent.setPlayerScore("p1", 0);

    var roomNoCurrentRound = new RoundEntity();
    roomNoCurrentRound.setRoom(roomNoCurrent);
    roomNoCurrentRound.setRoundNumber(1);
    roomNoCurrentRound.setTargetWord("PIZZA");
    roomNoCurrentRound.setMaxAttempts(6);
    roomNoCurrentRound.setRoundStatus(RoundStatus.PLAYING);
    roomNoCurrent.addRound(roomNoCurrentRound);

    roomJpaRepository.save(room1);
    roomJpaRepository.save(room2);
    roomJpaRepository.save(roomNoCurrent);

    // Act
    var found = roundJpaRepository.findCurrentRoundsWithDetailsByRoomIds(
        List.of("room-1", "room-2", "room-no-current"));

    // Assert
    assertThat(found)
        .extracting(r -> r.getRoom().getId() + ":" + r.getRoundNumber())
        .containsExactlyInAnyOrder("room-1:1", "room-2:2");

    var foundRoom1Round = found.stream()
        .filter(r -> r.getRoom().getId().equals("room-1"))
        .findFirst()
        .orElseThrow();
    assertThat(foundRoom1Round.getGuesses()).hasSize(1);
    assertThat(foundRoom1Round.getGuesses().getFirst().getLetters()).hasSize(1);
  }
}
