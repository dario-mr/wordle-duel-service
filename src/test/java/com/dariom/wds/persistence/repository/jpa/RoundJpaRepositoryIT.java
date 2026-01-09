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
}
