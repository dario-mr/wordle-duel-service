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
import static com.dariom.wds.websocket.model.EventType.ROUND_STARTED;
import static com.dariom.wds.websocket.model.EventType.SCORES_UPDATED;

import com.dariom.wds.config.WordleProperties;
import com.dariom.wds.domain.Language;
import com.dariom.wds.domain.Room;
import com.dariom.wds.domain.RoomStatus;
import com.dariom.wds.domain.Round;
import com.dariom.wds.exception.DictionaryEmptyException;
import com.dariom.wds.exception.InvalidGuessException;
import com.dariom.wds.exception.PlayerNotInRoomException;
import com.dariom.wds.exception.RoomNotFoundException;
import com.dariom.wds.persistence.entity.GuessEntity;
import com.dariom.wds.persistence.entity.LetterResultEmbeddable;
import com.dariom.wds.persistence.entity.RoomEntity;
import com.dariom.wds.persistence.entity.RoundEntity;
import com.dariom.wds.persistence.repository.DictionaryRepository;
import com.dariom.wds.persistence.repository.jpa.RoomJpaRepository;
import com.dariom.wds.persistence.repository.jpa.RoundJpaRepository;
import com.dariom.wds.websocket.model.RoomEvent;
import com.dariom.wds.websocket.model.RoomEventToPublish;
import com.dariom.wds.websocket.model.RoundFinishedPayload;
import com.dariom.wds.websocket.model.RoundStartedPayload;
import com.dariom.wds.websocket.model.ScoresUpdatedPayload;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoundService {

  private final DictionaryRepository dictionaryRepository;
  private final RoomJpaRepository roomJpaRepository;
  private final RoundJpaRepository roundJpaRepository;
  private final WordleProperties properties;
  private final WordleEvaluator evaluator;
  private final DomainMapper domainMapper;
  private final ApplicationEventPublisher applicationEventPublisher;
  private final Clock clock;

  @Transactional(readOnly = true)
  public Optional<Round> getCurrentRound(String roomId, Integer currentRoundNumber) {
    if (currentRoundNumber == null) {
      return Optional.empty();
    }

    return roundJpaRepository.findWithDetailsByRoomIdAndRoundNumber(roomId, currentRoundNumber)
        .map(domainMapper::toRound);
  }

  @Transactional
  public Round startNewRound(String roomId) {
    var room = findRoom(roomId);
    var round = startNewRoundEntity(room);
    roomJpaRepository.save(room);
    return domainMapper.toRound(round);
  }

  @Transactional
  public Room handleGuess(String roomId, String playerId, String guess) {
    var roomEntity = findRoom(roomId);

    validateRoomStatus(playerId, roomEntity);

    var roundEntity = ensureActiveRound(roomEntity);
    var normalizedGuess = normalizeGuess(guess);

    validatePlayerStatus(roomId, playerId, roundEntity);

    var attemptNumber = nextAttemptNumber(playerId, roundEntity);
    if (attemptNumber > roundEntity.getMaxAttempts()) {
      throw new InvalidGuessException(NO_ATTEMPTS_LEFT, "No attempts left for this round");
    }

    validateGuessFormat(normalizedGuess);
    validateGuessAllowed(normalizedGuess, roomEntity.getLanguage());

    List<LetterResultEmbeddable> letters =
        evaluator.evaluate(roundEntity.getTargetWord(), normalizedGuess);

    roundEntity.addGuess(
        createGuessEntity(roundEntity, playerId, normalizedGuess, attemptNumber, letters));
    updatePlayerStatusAfterGuess(roundEntity, playerId, normalizedGuess, attemptNumber);

    if (!roundEntity.isFinished() && isRoundFinished(roomEntity, roundEntity)) {
      finishRound(roundEntity, roomEntity);
    }

    roomJpaRepository.save(roomEntity);
    return domainMapper.toRoom(roomEntity, domainMapper.toRound(roundEntity));
  }

  private RoomEntity findRoom(String roomId) {
    return roomJpaRepository.findWithPlayersAndScoresById(roomId)
        .orElseThrow(() -> new RoomNotFoundException(roomId));
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
    if (room.getCurrentRoundNumber() == null) {
      return startNewRoundEntity(room);
    }

    return roundJpaRepository
        .findWithDetailsByRoomIdAndRoundNumber(room.getId(), room.getCurrentRoundNumber())
        .filter(round -> !round.isFinished())
        .orElseGet(() -> startNewRoundEntity(room));
  }

  private RoundEntity startNewRoundEntity(RoomEntity room) {
    if (room.getPlayerIds().size() != 2) {
      throw new IllegalStateException("Cannot start round unless room has 2 players");
    }

    var roomId = room.getId();
    var language = room.getLanguage();
    var targetWord = randomTargetWord(language);
    var nextRoundNumber =
        room.getCurrentRoundNumber() == null ? 1 : room.getCurrentRoundNumber() + 1;

    var round = new RoundEntity();
    round.setRoom(room);
    round.setRoundNumber(nextRoundNumber);
    round.setTargetWord(targetWord);
    round.setMaxAttempts(properties.maxAttempts());
    round.setFinished(false);
    round.setStartedAt(Instant.now(clock));

    for (var playerId : room.getPlayerIds()) {
      round.setPlayerStatus(playerId, PLAYING);
    }

    room.addRound(round);
    room.setCurrentRoundNumber(nextRoundNumber);

    publishRoundStarted(roomId, round.getRoundNumber(), round.getMaxAttempts());
    return round;
  }

  private String normalizeGuess(String guess) {
    return guess.strip().toUpperCase(Locale.ROOT);
  }

  private void validateGuessFormat(String guess) {
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
  }

  private void validateGuessAllowed(String guess, Language language) {
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

  private String randomTargetWord(Language language) {
    var answers = dictionaryRepository.getAnswerWords(language);
    if (answers.isEmpty()) {
      throw new DictionaryEmptyException(language);
    }

    var answersArray = answers.toArray(String[]::new);
    var randomIndex = ThreadLocalRandom.current().nextInt(answersArray.length);
    return answersArray[randomIndex];
  }

  private void publishRoundStarted(String roomId, int roundNumber, int maxAttempts) {
    applicationEventPublisher.publishEvent(new RoomEventToPublish(roomId, new RoomEvent(
        ROUND_STARTED,
        new RoundStartedPayload(roundNumber, maxAttempts)
    )));
  }
}
