package com.dariom.wds.service.round;

import static com.dariom.wds.api.v1.error.ErrorCode.NO_ATTEMPTS_LEFT;
import static com.dariom.wds.domain.RoundPlayerStatus.LOST;
import static com.dariom.wds.domain.RoundPlayerStatus.WON;
import static com.dariom.wds.service.round.validation.PlayerStatusValidator.validatePlayerStatus;

import com.dariom.wds.exception.InvalidGuessException;
import com.dariom.wds.persistence.entity.GuessEntity;
import com.dariom.wds.persistence.entity.LetterResultEmbeddable;
import com.dariom.wds.persistence.entity.RoomEntity;
import com.dariom.wds.persistence.entity.RoundEntity;
import com.dariom.wds.service.WordleEvaluator;
import com.dariom.wds.service.round.validation.GuessValidator;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class GuessSubmissionService {

  private final GuessValidator guessValidator;
  private final WordleEvaluator evaluator;
  private final Clock clock;

  public void applyGuess(
      String roomId,
      String playerId,
      String rawGuess,
      RoomEntity room,
      RoundEntity round) {
    var guess = normalizeGuess(rawGuess);
    validatePlayerStatus(roomId, playerId, round.getPlayerStatus(playerId));

    var attemptNumber = nextAttemptNumber(playerId, round);
    if (attemptNumber > round.getMaxAttempts()) {
      throw new InvalidGuessException(NO_ATTEMPTS_LEFT, "No attempts left for this round");
    }

    guessValidator.validateGuess(guess, room.getLanguage());

    var letters = evaluator.evaluate(round.getTargetWord(), guess);

    var guessEntity = createGuessEntity(round, playerId, guess, attemptNumber, letters);
    round.addGuess(guessEntity);

    updatePlayerStatusAfterGuess(round, playerId, guess, attemptNumber);
  }

  private String normalizeGuess(String guess) {
    return guess.strip().toUpperCase(Locale.ROOT);
  }

  private int nextAttemptNumber(String playerId, RoundEntity round) {
    var previousAttemptCount = (int) round.getGuesses().stream()
        .filter(g -> Objects.equals(g.getPlayerId(), playerId))
        .count();
    return previousAttemptCount + 1;
  }

  private GuessEntity createGuessEntity(
      RoundEntity round,
      String playerId,
      String guess,
      int attemptNumber,
      List<LetterResultEmbeddable> letters) {
    var guessEntity = new GuessEntity();
    guessEntity.setRound(round);
    guessEntity.setPlayerId(playerId);
    guessEntity.setWord(guess);
    guessEntity.setAttemptNumber(attemptNumber);
    guessEntity.setCreatedAt(Instant.now(clock));
    guessEntity.setLetters(letters);

    return guessEntity;
  }

  private void updatePlayerStatusAfterGuess(
      RoundEntity round,
      String playerId,
      String guess,
      int attemptNumber) {
    if (guess.equals(round.getTargetWord())) {
      round.setPlayerStatus(playerId, WON);
    } else if (attemptNumber >= round.getMaxAttempts()) {
      round.setPlayerStatus(playerId, LOST);
    }
  }
}
