package com.dariom.wds.service;

import static com.dariom.wds.api.v1.error.ErrorCode.INVALID_CHARS;
import static com.dariom.wds.api.v1.error.ErrorCode.INVALID_LANGUAGE;
import static com.dariom.wds.api.v1.error.ErrorCode.INVALID_LENGTH;
import static com.dariom.wds.api.v1.error.ErrorCode.NO_ATTEMPTS_LEFT;
import static com.dariom.wds.api.v1.error.ErrorCode.PLAYER_DONE;
import static com.dariom.wds.api.v1.error.ErrorCode.ROOM_NOT_IN_PROGRESS;
import static com.dariom.wds.api.v1.error.ErrorCode.WORD_NOT_ALLOWED;
import static com.dariom.wds.domain.RoundPlayerStatus.LOST;
import static com.dariom.wds.domain.RoundPlayerStatus.PLAYING;
import static com.dariom.wds.domain.RoundPlayerStatus.WON;
import static com.dariom.wds.websocket.model.EventType.ROUND_FINISHED;
import static com.dariom.wds.websocket.model.EventType.SCORES_UPDATED;

import com.dariom.wds.config.WordleProperties;
import com.dariom.wds.domain.Language;
import com.dariom.wds.domain.RoomStatus;
import com.dariom.wds.exception.InvalidGuessException;
import com.dariom.wds.exception.PlayerNotInRoomException;
import com.dariom.wds.exception.RoomNotFoundException;
import com.dariom.wds.persistence.entity.GuessEntity;
import com.dariom.wds.persistence.entity.RoomEntity;
import com.dariom.wds.persistence.entity.RoundEntity;
import com.dariom.wds.persistence.repository.DictionaryRepository;
import com.dariom.wds.persistence.repository.jpa.RoomJpaRepository;
import com.dariom.wds.websocket.RoomEventPublisher;
import com.dariom.wds.websocket.model.RoomEvent;
import com.dariom.wds.websocket.model.RoundFinishedPayload;
import com.dariom.wds.websocket.model.ScoresUpdatedPayload;
import java.time.Instant;
import java.util.Objects;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GameService {

  private final RoomJpaRepository roomJpaRepository;
  private final RoomLockManager roomLockManager;
  private final RoundLifecycleService roundLifecycleService;
  private final DictionaryRepository dictionaryRepository;
  private final WordleEvaluator evaluator;
  private final RoomEventPublisher eventPublisher;
  private final WordleProperties properties;

  @Transactional
  public RoomEntity handleGuess(String roomId, String playerId, String guess) {
    return roomLockManager.withRoomLock(roomId, () -> {
      var room = roomJpaRepository.findWithDetailsById(roomId)
          .orElseThrow(() -> new RoomNotFoundException(roomId));

      validateRoomStatus(playerId, room);

      var round = ensureActiveRound(room);
      if (round.isFinished()) {
        round = roundLifecycleService.startNewRound(room);
      }

      validateGuess(guess, room.getLanguage());
      validatePlayerStatus(roomId, playerId, round);

      var attemptNumber = getAttemptNumber(playerId, round);
      if (attemptNumber > round.getMaxAttempts()) {
        throw new InvalidGuessException(NO_ATTEMPTS_LEFT, "No attempts left for this round");
      }

      var letters = evaluator.evaluate(round.getTargetWord(), guess);

      var guessEntity = new GuessEntity();
      guessEntity.setRound(round);
      guessEntity.setPlayerId(playerId);
      guessEntity.setWord(guess);
      guessEntity.setAttemptNumber(attemptNumber);
      guessEntity.setCreatedAt(Instant.now());
      guessEntity.setLetters(letters);

      round.getGuesses().add(guessEntity);

      if (guess.equalsIgnoreCase(round.getTargetWord())) {
        round.setPlayerStatus(playerId, WON);
      } else if (attemptNumber >= round.getMaxAttempts()) {
        round.setPlayerStatus(playerId, LOST);
      }

      var roundFinishedNow = !round.isFinished() && isRoundFinished(room, round);
      if (roundFinishedNow) {
        finishRound(round, room);
      }

      return roomJpaRepository.save(room);
    });
  }

  private void validateRoomStatus(String playerId, RoomEntity room) {
    if (room.getStatus() != RoomStatus.IN_PROGRESS) {
      throw new InvalidGuessException(ROOM_NOT_IN_PROGRESS, "Room is not in progress");
    }

    if (!room.getPlayerIds().contains(playerId)) {
      throw new PlayerNotInRoomException(playerId, room.getId());
    }
  }

  private RoundEntity ensureActiveRound(RoomEntity room) {
    var round = roundLifecycleService.getCurrentRoundOrNull(room);
    if (round == null) {
      round = roundLifecycleService.startNewRound(room);
    }
    return round;
  }

  private void validateGuess(String guess, Language language) {
    if (guess.length() != properties.wordLength()) {
      throw new InvalidGuessException(INVALID_LENGTH,
          "Word must be %s characters".formatted(properties.wordLength()));
    }

    for (int i = 0; i < guess.length(); i++) {
      char c = guess.charAt(i);
      if (c < 'A' || c > 'Z') {
        throw new InvalidGuessException(INVALID_CHARS, "Word must contain only letters A-Z");
      }
    }

    if (language == null) {
      throw new InvalidGuessException(INVALID_LANGUAGE, "Room language not set");
    }

    var allowed = dictionaryRepository.getAllowedGuesses(language);
    if (!allowed.contains(guess)) {
      throw new InvalidGuessException(WORD_NOT_ALLOWED, "Word is not in dictionary");
    }
  }

  private void validatePlayerStatus(String roomId, String playerId, RoundEntity round) {
    var playerStatus = round.getPlayerStatus(playerId);

    if (playerStatus == null) {
      throw new PlayerNotInRoomException(playerId, roomId);
    }

    if (playerStatus != PLAYING) {
      throw new InvalidGuessException(PLAYER_DONE, "Player already finished this round");
    }
  }

  private int getAttemptNumber(String playerId, RoundEntity round) {
    var previousAttempts = (int) round.getGuesses().stream()
        .filter(g -> Objects.equals(g.getPlayerId(), playerId))
        .count();
    return previousAttempts + 1;
  }

  private boolean isRoundFinished(RoomEntity room, RoundEntity round) {
    for (var playerId : room.getPlayerIds()) {
      if (round.getPlayerStatus(playerId) == PLAYING) {
        return false;
      }
    }
    return true;
  }

  private void finishRound(RoundEntity round, RoomEntity room) {
    round.setFinished(true);
    round.setFinishedAt(Instant.now());

    for (var pid : room.getPlayerIds()) {
      if (round.getPlayerStatus(pid) == WON) {
        room.getScoresByPlayerId().merge(pid, 1, Integer::sum);
      }
    }

    eventPublisher.publish(room.getId(), new RoomEvent(
        ROUND_FINISHED,
        new RoundFinishedPayload(round.getRoundNumber())
    ));

    var scoresSnapshot = new TreeMap<>(room.getScoresByPlayerId());
    eventPublisher.publish(room.getId(), new RoomEvent(
        SCORES_UPDATED,
        new ScoresUpdatedPayload(scoresSnapshot)
    ));
  }
}
