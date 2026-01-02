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
import com.dariom.wds.domain.Room;
import com.dariom.wds.domain.RoomStatus;
import com.dariom.wds.exception.InvalidGuessException;
import com.dariom.wds.exception.PlayerNotInRoomException;
import com.dariom.wds.exception.RoomNotFoundException;
import com.dariom.wds.persistence.entity.GuessEntity;
import com.dariom.wds.persistence.entity.RoomEntity;
import com.dariom.wds.persistence.entity.RoundEntity;
import com.dariom.wds.persistence.repository.DictionaryRepository;
import com.dariom.wds.persistence.repository.jpa.RoomJpaRepository;
import com.dariom.wds.websocket.model.RoomEvent;
import com.dariom.wds.websocket.model.RoomEventToPublish;
import com.dariom.wds.websocket.model.RoundFinishedPayload;
import com.dariom.wds.websocket.model.ScoresUpdatedPayload;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
  private final DomainMapper domainMapper;
  private final ApplicationEventPublisher applicationEventPublisher;
  private final WordleProperties properties;
  private final Clock clock;

  @Transactional
  public Room handleGuess(String roomId, String playerId, String guess) {
    return roomLockManager.withRoomLock(roomId, () -> {
      var roomEntity = roomJpaRepository.findWithPlayersAndScoresById(roomId)
          .orElseThrow(() -> new RoomNotFoundException(roomId));

      validateRoomStatus(playerId, roomEntity);

      var roundEntity = ensureActiveRound(roomEntity);
      if (roundEntity.isFinished()) {
        roundEntity = roundLifecycleService.startNewRound(roomEntity);
      }
      roundEntity.setRoom(roomEntity);

      var normalizedGuess = normalizeGuess(guess);
      validateGuess(normalizedGuess, roomEntity.getLanguage());
      validatePlayerStatus(roomId, playerId, roundEntity);

      var attemptNumber = getAttemptNumber(playerId, roundEntity);
      if (attemptNumber > roundEntity.getMaxAttempts()) {
        throw new InvalidGuessException(NO_ATTEMPTS_LEFT, "No attempts left for this round");
      }

      var letters = evaluator.evaluate(roundEntity.getTargetWord(), normalizedGuess);

      var guessEntity = new GuessEntity();
      guessEntity.setRound(roundEntity);
      guessEntity.setPlayerId(playerId);
      guessEntity.setWord(normalizedGuess);
      guessEntity.setAttemptNumber(attemptNumber);
      guessEntity.setCreatedAt(Instant.now(clock));
      guessEntity.setLetters(letters);

      roundEntity.addGuess(guessEntity);

      if (normalizedGuess.equals(roundEntity.getTargetWord())) {
        roundEntity.setPlayerStatus(playerId, WON);
      } else if (attemptNumber >= roundEntity.getMaxAttempts()) {
        roundEntity.setPlayerStatus(playerId, LOST);
      }

      var roundFinishedNow = !roundEntity.isFinished() && isRoundFinished(roomEntity, roundEntity);
      if (roundFinishedNow) {
        finishRound(roundEntity, roomEntity);
      }

      roomJpaRepository.save(roomEntity);
      return domainMapper.toRoom(roomEntity, domainMapper.toRound(roundEntity));
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
    var round = roundLifecycleService.getCurrentRoundEntityOrNull(room);
    if (round == null) {
      round = roundLifecycleService.startNewRound(room);
    }
    return round;
  }

  private String normalizeGuess(String guess) {
    return guess.trim().toUpperCase();
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
    round.setFinishedAt(Instant.now(clock));

    for (var pid : room.getPlayerIds()) {
      if (round.getPlayerStatus(pid) == WON) {
        room.getScoresByPlayerId().merge(pid, 1, Integer::sum);
      }
    }

    applicationEventPublisher.publishEvent(new RoomEventToPublish(room.getId(), new RoomEvent(
        ROUND_FINISHED,
        new RoundFinishedPayload(round.getRoundNumber())
    )));

    var scoresSnapshot = new TreeMap<>(room.getScoresByPlayerId());
    applicationEventPublisher.publishEvent(new RoomEventToPublish(room.getId(), new RoomEvent(
        SCORES_UPDATED,
        new ScoresUpdatedPayload(scoresSnapshot)
    )));
  }
}
