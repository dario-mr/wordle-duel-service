package com.dariom.wds.service.round;

import static com.dariom.wds.domain.RoundPlayerStatus.PLAYING;
import static com.dariom.wds.domain.RoundPlayerStatus.WON;
import static com.dariom.wds.websocket.model.EventType.ROUND_FINISHED;
import static com.dariom.wds.websocket.model.EventType.ROUND_STARTED;
import static com.dariom.wds.websocket.model.EventType.SCORES_UPDATED;

import com.dariom.wds.config.WordleProperties;
import com.dariom.wds.domain.Language;
import com.dariom.wds.exception.DictionaryEmptyException;
import com.dariom.wds.exception.RoomNotReadyException;
import com.dariom.wds.persistence.entity.RoomEntity;
import com.dariom.wds.persistence.entity.RoundEntity;
import com.dariom.wds.persistence.repository.DictionaryRepository;
import com.dariom.wds.persistence.repository.jpa.RoundJpaRepository;
import com.dariom.wds.websocket.model.RoomEvent;
import com.dariom.wds.websocket.model.RoomEventToPublish;
import com.dariom.wds.websocket.model.RoundFinishedPayload;
import com.dariom.wds.websocket.model.RoundStartedPayload;
import com.dariom.wds.websocket.model.ScoresUpdatedPayload;
import java.time.Clock;
import java.time.Instant;
import java.util.TreeMap;
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
  private final ApplicationEventPublisher applicationEventPublisher;
  private final Clock clock;

  public RoundEntity ensureActiveRound(RoomEntity room) {
    if (room.getCurrentRoundNumber() == null) {
      return startNewRoundEntity(room);
    }

    return roundJpaRepository
        .findWithDetailsByRoomIdAndRoundNumber(room.getId(), room.getCurrentRoundNumber())
        .filter(round -> !round.isFinished())
        .orElseGet(() -> startNewRoundEntity(room));
  }

  public RoundEntity startNewRoundEntity(RoomEntity room) {
    var playerCount = room.getPlayerIds().size();
    if (playerCount != 2) {
      throw new RoomNotReadyException(room.getId(), playerCount);
    }

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

    publishRoomEvent(room.getId(), new RoomEvent(
        ROUND_STARTED,
        new RoundStartedPayload(round.getRoundNumber(), round.getMaxAttempts())
    ));

    return round;
  }

  public boolean isRoundFinished(RoomEntity room, RoundEntity round) {
    for (var playerId : room.getPlayerIds()) {
      if (round.getPlayerStatus(playerId) == PLAYING) {
        return false;
      }
    }
    return true;
  }

  public void finishRound(RoundEntity round, RoomEntity room) {
    round.setFinished(true);
    round.setFinishedAt(Instant.now(clock));

    for (var pid : room.getPlayerIds()) {
      if (round.getPlayerStatus(pid) == WON) {
        room.incrementPlayerScore(pid, 1);
      }
    }

    publishRoomEvent(room.getId(), new RoomEvent(
        ROUND_FINISHED,
        new RoundFinishedPayload(round.getRoundNumber())
    ));

    var scoresSnapshot = new TreeMap<>(room.getScoresByPlayerId());
    publishRoomEvent(room.getId(), new RoomEvent(
        SCORES_UPDATED,
        new ScoresUpdatedPayload(scoresSnapshot)
    ));
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
    applicationEventPublisher.publishEvent(new RoomEventToPublish(roomId, roomEvent));
  }
}
