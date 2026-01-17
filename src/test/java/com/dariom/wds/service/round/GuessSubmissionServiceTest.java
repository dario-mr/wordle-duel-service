package com.dariom.wds.service.round;

import static com.dariom.wds.api.common.ErrorCode.NO_ATTEMPTS_LEFT;
import static com.dariom.wds.domain.Language.IT;
import static com.dariom.wds.domain.LetterStatus.CORRECT;
import static com.dariom.wds.domain.LetterStatus.PRESENT;
import static com.dariom.wds.domain.RoundPlayerStatus.LOST;
import static com.dariom.wds.domain.RoundPlayerStatus.PLAYING;
import static com.dariom.wds.domain.RoundPlayerStatus.WON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dariom.wds.domain.LetterResult;
import com.dariom.wds.exception.InvalidGuessException;
import com.dariom.wds.persistence.entity.GuessEntity;
import com.dariom.wds.persistence.entity.RoomEntity;
import com.dariom.wds.persistence.entity.RoundEntity;
import com.dariom.wds.service.WordleEvaluator;
import com.dariom.wds.service.round.validation.GuessValidator;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GuessSubmissionServiceTest {

  @Mock
  private GuessValidator guessValidator;
  @Mock
  private WordleEvaluator evaluator;
  private final Clock clock = Clock.fixed(Instant.parse("2025-01-01T12:00:00Z"), ZoneOffset.UTC);

  private GuessSubmissionService service;

  @BeforeEach
  void setUp() {
    service = new GuessSubmissionService(clock, evaluator, guessValidator);
  }

  @Test
  void applyGuess_rawGuessHasWhitespace_storesNormalizedGuessAndValidatesNormalizedValue() {
    // Arrange
    var room = new RoomEntity();
    room.setLanguage(IT);

    var round = new RoundEntity();
    round.setTargetWord("PIZZA");
    round.setMaxAttempts(6);
    round.setPlayerStatus("p1", PLAYING);

    var evaluated = List.of(
        new LetterResult('P', CORRECT),
        new LetterResult('I', CORRECT),
        new LetterResult('Z', CORRECT),
        new LetterResult('Z', CORRECT),
        new LetterResult('A', CORRECT)
    );
    when(evaluator.evaluate(anyString(), anyString())).thenReturn(evaluated);

    // Act
    var statusUpdate = service.applyGuess("room-1", "p1", "  pizza  ", room, round);

    // Assert
    verify(guessValidator).validateGuess("PIZZA", "PIZZA", IT);
    verify(evaluator).evaluate("PIZZA", "PIZZA");
    assertThat(statusUpdate).contains(WON);

    assertThat(round.getGuesses()).hasSize(1);

    var stored = round.getGuesses().getFirst();
    assertThat(stored.getWord()).isEqualTo("PIZZA");
    assertThat(stored.getAttemptNumber()).isEqualTo(1);
    assertThat(stored.getCreatedAt()).isEqualTo(clock.instant());
    assertThat(stored.getLetters())
        .extracting(l -> "%s:%s".formatted(l.getLetter(), l.getStatus()))
        .containsExactly("P:CORRECT", "I:CORRECT", "Z:CORRECT", "Z:CORRECT", "A:CORRECT");
    assertThat(round.getPlayerStatus("p1")).isEqualTo(WON);
  }

  @Test
  void applyGuess_attemptExceedsMax_throwsInvalidGuessException() {
    // Arrange
    var room = new RoomEntity();
    room.setLanguage(IT);

    var round = new RoundEntity();
    round.setTargetWord("PIZZA");
    round.setMaxAttempts(1);
    round.setPlayerStatus("p1", PLAYING);

    round.addGuess(previousGuess(round, "p1", 1));

    // Act
    var thrown = catchThrowable(() -> service.applyGuess("room-1", "p1", "pizza", room, round));

    // Assert
    assertThat(thrown)
        .isInstanceOfSatisfying(
            InvalidGuessException.class,
            ex -> assertThat(ex.getCode()).isEqualTo(NO_ATTEMPTS_LEFT));
  }

  @Test
  void applyGuess_lastAttemptNotCorrect_setsPlayerStatusLost() {
    // Arrange
    var room = new RoomEntity();
    room.setLanguage(IT);

    var round = new RoundEntity();
    round.setTargetWord("PIZZA");
    round.setMaxAttempts(1);
    round.setPlayerStatus("p1", PLAYING);

    when(evaluator.evaluate(anyString(), anyString())).thenReturn(List.of(
        new LetterResult('P', PRESENT)
    ));

    // Act
    var statusUpdate = service.applyGuess("room-1", "p1", "pasta", room, round);

    // Assert
    assertThat(statusUpdate).contains(LOST);
    assertThat(round.getPlayerStatus("p1")).isEqualTo(LOST);
  }

  private static GuessEntity previousGuess(RoundEntity round, String playerId, int attemptNumber) {
    var guess = new GuessEntity();
    guess.setRound(round);
    guess.setPlayerId(playerId);
    guess.setWord("PASTA");
    guess.setAttemptNumber(attemptNumber);
    return guess;
  }
}
