package com.dariom.wds.service.round;

import static com.dariom.wds.api.common.ErrorCode.NO_ATTEMPTS_LEFT;
import static com.dariom.wds.domain.RoundPlayerStatus.LOST;
import static com.dariom.wds.domain.RoundPlayerStatus.WON;
import static com.dariom.wds.service.round.validation.PlayerStatusValidator.validatePlayerStatus;
import static java.util.stream.Collectors.toCollection;

import com.dariom.wds.domain.LetterResult;
import com.dariom.wds.domain.RoundPlayerStatus;
import com.dariom.wds.exception.InvalidGuessException;
import com.dariom.wds.persistence.entity.GuessEntity;
import com.dariom.wds.persistence.entity.LetterResultEmbeddable;
import com.dariom.wds.persistence.entity.RoomEntity;
import com.dariom.wds.persistence.entity.RoundEntity;
import com.dariom.wds.service.WordleEvaluator;
import com.dariom.wds.service.round.validation.GuessValidator;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class GuessSubmissionService {

  private final Clock clock;
  private final WordleEvaluator evaluator;
  private final GuessValidator guessValidator;

  public Optional<RoundPlayerStatus> applyGuess(
      String roomId,
      String playerId,
      String rawGuess,
      RoomEntity room,
      RoundEntity round) {
    var guess = normalizeGuess(rawGuess);
    validatePlayerStatus(roomId, playerId, round.getPlayerStatus(playerId));

    var attemptNumber = round.nextAttemptNumber(playerId);
    if (attemptNumber > round.getMaxAttempts()) {
      throw new InvalidGuessException(NO_ATTEMPTS_LEFT, "No attempts left for this round");
    }

    guessValidator.validateGuess(guess, round.getTargetWord(), room.getLanguage());

    var letterResults = evaluator.evaluate(round.getTargetWord(), guess);

    var guessEntity = createGuessEntity(round, playerId, guess, attemptNumber, letterResults);
    round.addGuess(guessEntity);

    return updatePlayerStatusAfterGuess(round, playerId, guess, attemptNumber);
  }

  private String normalizeGuess(String guess) {
    return guess.strip().toUpperCase(Locale.ROOT);
  }

  private GuessEntity createGuessEntity(
      RoundEntity round,
      String playerId,
      String guess,
      int attemptNumber,
      List<LetterResult> letterResults) {
    var letterResultsEntity = letterResults.stream()
        .map(lr -> new LetterResultEmbeddable(lr.letter(), lr.status()))
        .collect(toCollection(ArrayList::new));

    var guessEntity = new GuessEntity();
    guessEntity.setRound(round);
    guessEntity.setPlayerId(playerId);
    guessEntity.setWord(guess);
    guessEntity.setAttemptNumber(attemptNumber);
    guessEntity.setCreatedAt(Instant.now(clock));
    guessEntity.setLetters(letterResultsEntity);

    return guessEntity;
  }

  private Optional<RoundPlayerStatus> updatePlayerStatusAfterGuess(
      RoundEntity round,
      String playerId,
      String guess,
      int attemptNumber) {
    if (guess.equals(round.getTargetWord())) {
      round.setPlayerStatus(playerId, WON);
      return Optional.of(WON);
    }

    if (attemptNumber >= round.getMaxAttempts()) {
      round.setPlayerStatus(playerId, LOST);
      return Optional.of(LOST);
    }

    return Optional.empty();
  }
}
