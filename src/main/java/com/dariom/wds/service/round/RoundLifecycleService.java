package com.dariom.wds.service.round;

import static com.dariom.wds.domain.RoundPlayerStatus.PLAYING;
import static com.dariom.wds.websocket.model.EventType.ROUND_STARTED;

import com.dariom.wds.config.WordleProperties;
import com.dariom.wds.domain.Language;
import com.dariom.wds.exception.DictionaryEmptyException;
import com.dariom.wds.persistence.entity.RoomEntity;
import com.dariom.wds.persistence.entity.RoundEntity;
import com.dariom.wds.persistence.repository.DictionaryRepository;
import com.dariom.wds.persistence.repository.jpa.RoundJpaRepository;
import com.dariom.wds.websocket.model.RoomEvent;
import com.dariom.wds.websocket.model.RoomEventToPublish;
import com.dariom.wds.websocket.model.RoundStartedPayload;
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
