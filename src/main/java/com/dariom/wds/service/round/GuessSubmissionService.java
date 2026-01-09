package com.dariom.wds.service.round;

import static com.dariom.wds.api.v1.error.ErrorCode.NO_ATTEMPTS_LEFT;
import static com.dariom.wds.domain.RoundPlayerStatus.LOST;
import static com.dariom.wds.domain.RoundPlayerStatus.WON;
import static com.dariom.wds.service.round.validation.PlayerStatusValidator.validatePlayerStatus;
import static com.dariom.wds.websocket.model.EventType.SCORES_UPDATED;
import static java.util.stream.Collectors.toCollection;

import com.dariom.wds.domain.LetterResult;
import com.dariom.wds.exception.InvalidGuessException;
import com.dariom.wds.persistence.entity.GuessEntity;
import com.dariom.wds.persistence.entity.LetterResultEmbeddable;
import com.dariom.wds.persistence.entity.RoomEntity;
import com.dariom.wds.persistence.entity.RoundEntity;
import com.dariom.wds.service.WordleEvaluator;
import com.dariom.wds.service.round.validation.GuessValidator;
import com.dariom.wds.websocket.model.RoomEvent;
import com.dariom.wds.websocket.model.RoomEventToPublish;
import com.dariom.wds.websocket.model.ScoresUpdatedPayload;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class GuessSubmissionService {

  private final Clock clock;
  private final WordleEvaluator evaluator;
  private final GuessValidator guessValidator;
  private final ApplicationEventPublisher applicationEventPublisher;

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

    guessValidator.validateGuess(guess, round.getTargetWord(), room.getLanguage());

    var letterResults = evaluator.evaluate(round.getTargetWord(), guess);

    var guessEntity = createGuessEntity(round, playerId, guess, attemptNumber, letterResults);
    round.addGuess(guessEntity);

    updatePlayerStatusAfterGuess(round, playerId, guess, attemptNumber, room);
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

  private void updatePlayerStatusAfterGuess(
      RoundEntity round,
      String playerId,
      String guess,
      int attemptNumber,
      RoomEntity room) {
    if (guess.equals(round.getTargetWord())) {
      round.setPlayerStatus(playerId, WON);
      publishScoreUpdated(room);
    } else if (attemptNumber >= round.getMaxAttempts()) {
      round.setPlayerStatus(playerId, LOST);
      publishScoreUpdated(room);
    }
  }

  private void publishScoreUpdated(RoomEntity room) {
    var scoresSnapshot = new TreeMap<>(room.getScoresByPlayerId());
    applicationEventPublisher.publishEvent(new RoomEventToPublish(room.getId(),
        new RoomEvent(SCORES_UPDATED, new ScoresUpdatedPayload(scoresSnapshot)))
    );
  }
}
