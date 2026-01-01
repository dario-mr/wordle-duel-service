package com.dariom.wds.service;

import static com.dariom.wds.api.v1.error.ErrorCode.INVALID_CHARS;
import static com.dariom.wds.api.v1.error.ErrorCode.INVALID_LANGUAGE;
import static com.dariom.wds.api.v1.error.ErrorCode.INVALID_LENGTH;
import static com.dariom.wds.api.v1.error.ErrorCode.INVALID_PLAYER_ID;
import static com.dariom.wds.api.v1.error.ErrorCode.INVALID_WORD;
import static com.dariom.wds.api.v1.error.ErrorCode.NO_ATTEMPTS_LEFT;
import static com.dariom.wds.api.v1.error.ErrorCode.PLAYER_DONE;
import static com.dariom.wds.api.v1.error.ErrorCode.ROOM_NOT_IN_PROGRESS;
import static com.dariom.wds.api.v1.error.ErrorCode.WORD_NOT_ALLOWED;
import static com.dariom.wds.websocket.model.EventType.ROUND_FINISHED;
import static com.dariom.wds.websocket.model.EventType.SCORES_UPDATED;

import com.dariom.wds.domain.RoomStatus;
import com.dariom.wds.domain.RoundPlayerStatus;
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
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GameService {

  private static final int WORD_LENGTH = 5;

  private final RoomJpaRepository roomJpaRepository;
  private final RoomLockManager roomLockManager;
  private final RoundLifecycleService roundLifecycleService;
  private final DictionaryRepository dictionaryRepository;
  private final WordleEvaluator evaluator;
  private final RoomEventPublisher eventPublisher;

  @Transactional
  public RoomEntity handleGuess(String roomId, String playerId, String rawWord) {
    var normalizedPlayerId = normalizePlayerId(playerId);

    return roomLockManager.withRoomLock(roomId, () -> {
      var room = roomJpaRepository.findWithDetailsById(roomId)
          .orElseThrow(() -> new RoomNotFoundException(roomId));

      if (room.getStatus() != RoomStatus.IN_PROGRESS) {
        throw new InvalidGuessException(ROOM_NOT_IN_PROGRESS, "Room is not in progress");
      }

      if (!room.getPlayerIds().contains(normalizedPlayerId)) {
        throw new PlayerNotInRoomException(normalizedPlayerId, roomId);
      }

      var round = ensureActiveRound(room);
      if (round.isFinished()) {
        round = roundLifecycleService.startNewRound(room);
      }

      var guess = normalizeWord(rawWord);
      validateWord(guess, room);

      var playerStatus = round.getStatusByPlayerId().get(normalizedPlayerId);
      if (playerStatus == null) {
        throw new PlayerNotInRoomException(normalizedPlayerId, roomId);
      }
      if (playerStatus != RoundPlayerStatus.PLAYING) {
        throw new InvalidGuessException(PLAYER_DONE, "Player already finished this round");
      }

      int previousAttempts = (int) round.getGuesses().stream()
          .filter(g -> Objects.equals(g.getPlayerId(), normalizedPlayerId))
          .count();
      int attemptNumber = previousAttempts + 1;

      if (attemptNumber > round.getMaxAttempts()) {
        throw new InvalidGuessException(NO_ATTEMPTS_LEFT, "No attempts left for this round");
      }

      var letters = evaluator.evaluate(round.getTargetWord(), guess);

      var guessEntity = new GuessEntity();
      guessEntity.setRound(round);
      guessEntity.setPlayerId(normalizedPlayerId);
      guessEntity.setWord(guess);
      guessEntity.setAttemptNumber(attemptNumber);
      guessEntity.setCreatedAt(Instant.now());
      guessEntity.setLetters(letters);

      round.getGuesses().add(guessEntity);

      if (guess.equalsIgnoreCase(round.getTargetWord())) {
        round.getStatusByPlayerId().put(normalizedPlayerId, RoundPlayerStatus.WON);
      } else if (attemptNumber >= round.getMaxAttempts()) {
        round.getStatusByPlayerId().put(normalizedPlayerId, RoundPlayerStatus.LOST);
      }

      boolean roundFinishedNow = !round.isFinished() && isRoundFinished(room, round);
      if (roundFinishedNow) {
        round.setFinished(true);
        round.setFinishedAt(Instant.now());

        for (String pid : room.getPlayerIds()) {
          if (round.getStatusByPlayerId().get(pid) == RoundPlayerStatus.WON) {
            room.getScoresByPlayerId().merge(pid, 1, Integer::sum);
          }
        }

        eventPublisher.publish(room.getId(), new RoomEvent(
            ROUND_FINISHED,
            new RoundFinishedPayload(round.getRoundNumber())
        ));

        Map<String, Integer> snapshot = new TreeMap<>(room.getScoresByPlayerId());
        eventPublisher.publish(room.getId(), new RoomEvent(
            SCORES_UPDATED,
            new ScoresUpdatedPayload(snapshot)
        ));
      }

      return roomJpaRepository.save(room);
    });
  }

  private RoundEntity ensureActiveRound(RoomEntity room) {
    var round = roundLifecycleService.getCurrentRoundOrNull(room);
    if (round == null) {
      round = roundLifecycleService.startNewRound(room);
    }
    return round;
  }

  private void validateWord(String guess, RoomEntity room) {
    if (guess.length() != WORD_LENGTH) {
      throw new InvalidGuessException(INVALID_LENGTH,
          "Word must be " + WORD_LENGTH + " characters");
    }

    for (int i = 0; i < guess.length(); i++) {
      char c = guess.charAt(i);
      if (c < 'A' || c > 'Z') {
        throw new InvalidGuessException(INVALID_CHARS, "Word must contain only letters A-Z");
      }
    }

    var language = room.getLanguage();
    if (language == null) {
      throw new InvalidGuessException(INVALID_LANGUAGE, "Room language not set");
    }

    var allowed = dictionaryRepository.getAllowedGuesses(language);
    if (!allowed.contains(guess)) {
      throw new InvalidGuessException(WORD_NOT_ALLOWED, "Word is not in dictionary");
    }
  }

  private static String normalizeWord(String rawWord) {
    if (rawWord == null) {
      throw new InvalidGuessException(INVALID_WORD, "word is required");
    }

    var w = rawWord.trim().toUpperCase();
    if (w.isEmpty()) {
      throw new InvalidGuessException(INVALID_WORD, "word is required");
    }

    return w;
  }

  private static String normalizePlayerId(String playerId) {
    if (playerId == null || playerId.trim().isEmpty()) {
      throw new InvalidGuessException(INVALID_PLAYER_ID, "playerId is required");
    }
    return playerId.trim();
  }

  private static boolean isRoundFinished(RoomEntity room, RoundEntity round) {
    for (String pid : room.getPlayerIds()) {
      if (round.getStatusByPlayerId().get(pid) == RoundPlayerStatus.PLAYING) {
        return false;
      }
    }
    return true;
  }
}
