package com.dariom.wds.service.round;

import static com.dariom.wds.api.common.ErrorCode.PLAYER_DONE;
import static com.dariom.wds.domain.RoomStatus.CLOSED;
import static com.dariom.wds.domain.RoundPlayerStatus.PLAYING;
import static com.dariom.wds.domain.RoundPlayerStatus.WON;
import static com.dariom.wds.domain.RoundStatus.ENDED;
import static com.dariom.wds.websocket.model.EventType.ROOM_CLOSED;
import static com.dariom.wds.websocket.model.EventType.SCORES_UPDATED;

import com.dariom.wds.config.WordleProperties;
import com.dariom.wds.domain.Language;
import com.dariom.wds.domain.RoundPlayerStatus;
import com.dariom.wds.domain.RoundStatus;
import com.dariom.wds.exception.DictionaryEmptyException;
import com.dariom.wds.exception.InvalidGuessException;
import com.dariom.wds.exception.PlayerNotInRoomException;
import com.dariom.wds.exception.RoomNotReadyException;
import com.dariom.wds.persistence.entity.RoomEntity;
import com.dariom.wds.persistence.entity.RoundEntity;
import com.dariom.wds.persistence.repository.DictionaryRepository;
import com.dariom.wds.persistence.repository.jpa.RoundJpaRepository;
import com.dariom.wds.websocket.model.RoomEvent;
import com.dariom.wds.websocket.model.RoomEventToPublish;
import com.dariom.wds.websocket.model.ScoresUpdatedPayload;
import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class RoundLifecycleService {

  private final DictionaryRepository dictionaryRepository;
  private final RoundJpaRepository roundJpaRepository;
  private final WordleProperties properties;
  private final ApplicationEventPublisher eventPublisher;
  private final Clock clock;

  public RoundEntity ensurePlayerRound(RoomEntity room, String playerId) {
    var player = room.findRoomPlayer(playerId)
        .orElseThrow(() -> new PlayerNotInRoomException(playerId, room.getId()));
    var roundNumber = player.getCurrentRoundNumber();
    if (roundNumber == null) {
      throw new InvalidGuessException(PLAYER_DONE, "Player already finished the match");
    }

    var round = roundJpaRepository
        .findWithDetailsByRoomIdAndRoundNumber(room.getId(), roundNumber)
        .orElseGet(() -> createRound(room, roundNumber, playerId));

    if (round.getPlayerStatus(playerId) == null) {
      round.setPlayerStatus(playerId, PLAYING);
    }
    return round;
  }

  public RoundEntity startNewRoundEntity(RoomEntity room) {
    var playerCount = room.getPlayerIds().size();
    if (playerCount != 2) {
      throw new RoomNotReadyException(room.getId(), playerCount);
    }

    var nextRoundNumber = room.getRoomPlayers().stream()
        .map(player -> player.getCurrentRoundNumber())
        .filter(number -> number != null)
        .max(Integer::compareTo)
        .orElse(0) + 1;
    var round = createRound(room, nextRoundNumber, null);
    room.getRoomPlayers().forEach(player -> player.setCurrentRoundNumber(nextRoundNumber));
    return round;
  }

  public void completePlayerRound(
      RoundEntity round, RoomEntity room, String playerId, RoundPlayerStatus status) {
    if (status == WON) {
      room.incrementPlayerScore(playerId,
          round.getMaxAttempts() - round.currentAttemptNumber(playerId) + 1);
    }

    var finalRound = room.getConfiguredRounds() != null
        && room.getConfiguredRounds().isFinalRound(round.getRoundNumber());

    if (isRoundFinished(room, round)) {
      finishRound(round);
    }

    publishRoomEvent(room.getId(), new RoomEvent(
        SCORES_UPDATED,
        new ScoresUpdatedPayload(room.getScoresByPlayerId())
    ));

    if (finalRound && isRoundFinished(room, round)) {
      room.setStatus(CLOSED);
      publishRoomEvent(room.getId(), new RoomEvent(
          ROOM_CLOSED,
          new ScoresUpdatedPayload(room.getScoresByPlayerId())
      ));
    }
  }

  public RoundEntity advancePlayerRound(RoomEntity room, String playerId) {
    var player = room.findRoomPlayer(playerId)
        .orElseThrow(() -> new PlayerNotInRoomException(playerId, room.getId()));
    var roundNumber = player.getCurrentRoundNumber();
    if (roundNumber == null) {
      throw new InvalidGuessException(PLAYER_DONE, "Player already finished the match");
    }

    var round = roundJpaRepository
        .findWithDetailsByRoomIdAndRoundNumber(room.getId(), roundNumber)
        .orElseThrow(() -> new InvalidGuessException(PLAYER_DONE, "Player has no active round"));
    var playerStatus = round.getPlayerStatus(playerId);
    if (playerStatus == null) {
      throw new PlayerNotInRoomException(playerId, room.getId());
    }
    if (playerStatus == PLAYING) {
      throw new InvalidGuessException(PLAYER_DONE, "Player has not finished this round");
    }

    if (room.getConfiguredRounds() != null
        && room.getConfiguredRounds().isFinalRound(roundNumber)) {
      throw new InvalidGuessException(PLAYER_DONE, "Player already finished the match");
    }

    player.setCurrentRoundNumber(roundNumber + 1);
    return ensurePlayerRound(room, playerId);
  }

  public boolean isRoundFinished(RoomEntity room, RoundEntity round) {
    for (var playerId : room.getPlayerIds()) {
      var status = round.getPlayerStatus(playerId);
      if (status == null || status == PLAYING) {
        return false;
      }
    }
    return true;
  }

  public void finishRound(RoundEntity round) {
    round.setRoundStatus(ENDED);
    round.setFinishedAt(Instant.now(clock));
  }

  private RoundEntity createRound(RoomEntity room, int roundNumber, String playerId) {
    var targetWord = randomTargetWord(room.getLanguage());
    var round = new RoundEntity();
    round.setRoom(room);
    round.setRoundNumber(roundNumber);
    round.setTargetWord(targetWord);
    round.setMaxAttempts(properties.maxAttempts());
    round.setRoundStatus(RoundStatus.PLAYING);
    round.setStartedAt(Instant.now(clock));

    if (playerId == null) {
      room.getPlayerIds().forEach(id -> round.setPlayerStatus(id, PLAYING));
    } else {
      round.setPlayerStatus(playerId, PLAYING);
    }

    room.addRound(round);
    return round;
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

  private void publishRoomEvent(String roomId, RoomEvent roomEvent) {
    eventPublisher.publishEvent(new RoomEventToPublish(roomId, roomEvent));
  }
}
