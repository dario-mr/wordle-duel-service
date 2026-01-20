package com.dariom.wds.service;

import static com.dariom.wds.domain.Language.IT;
import static com.dariom.wds.domain.LetterStatus.ABSENT;
import static com.dariom.wds.domain.LetterStatus.CORRECT;
import static com.dariom.wds.domain.RoomStatus.WAITING_FOR_PLAYERS;
import static com.dariom.wds.domain.RoundPlayerStatus.PLAYING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.dariom.wds.domain.Guess;
import com.dariom.wds.domain.Player;
import com.dariom.wds.persistence.entity.GuessEntity;
import com.dariom.wds.persistence.entity.LetterResultEmbeddable;
import com.dariom.wds.persistence.entity.RoomEntity;
import com.dariom.wds.persistence.entity.RoundEntity;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DomainMapperTest {

  private final DomainMapper mapper = new DomainMapper();

  @Test
  void toRoom_unsortedPlayers_returnsSortedPlayersAndScores() {
    // Arrange
    var entity = new RoomEntity();
    entity.setId("room-1");
    entity.setLanguage(IT);
    entity.setStatus(WAITING_FOR_PLAYERS);

    entity.addPlayer("b");
    entity.addPlayer("a");

    entity.setPlayerScore("b", 1);
    entity.setPlayerScore("a", 2);

    var displayNamePerPlayer = Map.of("a", "John", "b", "Bart");

    // Act
    var room = mapper.toRoom(entity, null, displayNamePerPlayer);

    // Assert
    assertThat(room.id()).isEqualTo("room-1");
    assertThat(room.language()).isEqualTo(IT);
    assertThat(room.status()).isEqualTo(WAITING_FOR_PLAYERS);
    assertThat(room.currentRound()).isNull();
    assertThat(room.players().size()).isEqualTo(2);
    assertThat(room.players())
        .extracting(Player::id, Player::score, Player::displayName)
        .containsExactly(
            tuple("a", 2, "John"),
            tuple("b", 1, "Bart")
        );
  }

  @Test
  void toRoom_displayNameMapNull_stillMapsPlayersAndScores() {
    // Arrange
    var entity = new RoomEntity();
    entity.setId("room-1");
    entity.setLanguage(IT);
    entity.setStatus(WAITING_FOR_PLAYERS);

    entity.addPlayer("b");
    entity.addPlayer("a");

    entity.setPlayerScore("b", 1);
    entity.setPlayerScore("a", 2);

    // Act
    var room = mapper.toRoom(entity, null, null);

    // Assert
    assertThat(room.players())
        .extracting(Player::id, Player::score, Player::displayName)
        .containsExactly(
            tuple("a", 2, null),
            tuple("b", 1, null)
        );
  }

  @Test
  void toRound_unsortedGuesses_returnsGroupedGuessesSortedByAttemptNumber() {
    // Arrange
    var roundEntity = new RoundEntity();
    roundEntity.setRoundNumber(1);
    roundEntity.setMaxAttempts(6);
    roundEntity.setPlayerStatus("p1", PLAYING);

    var guess2 = guess(roundEntity, "p1", "PIZZA", 2, List.of(
        new LetterResultEmbeddable('P', CORRECT)
    ));
    var guess1 = guess(roundEntity, "p1", "PASTA", 1, List.of(
        new LetterResultEmbeddable('P', ABSENT)
    ));

    roundEntity.addGuess(guess2);
    roundEntity.addGuess(guess1);

    // Act
    var round = mapper.toRound(roundEntity);

    // Assert
    assertThat(round.guessesByPlayerId()).containsKey("p1");
    assertThat(round.guessesByPlayerId().get("p1"))
        .extracting(Guess::attemptNumber)
        .containsExactly(1, 2);

    assertThat(round.guessesByPlayerId().get("p1").getFirst().letters())
        .extracting(lr -> "%s:%s".formatted(lr.letter(), lr.status()))
        .containsExactly("P:ABSENT");
  }

  private static GuessEntity guess(
      RoundEntity round,
      String playerId,
      String word,
      int attemptNumber,
      List<LetterResultEmbeddable> letters
  ) {
    var guess = new GuessEntity();
    guess.setRound(round);
    guess.setPlayerId(playerId);
    guess.setWord(word);
    guess.setAttemptNumber(attemptNumber);
    guess.setLetters(letters);
    return guess;
  }
}
