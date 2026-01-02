package com.dariom.wds.service;

import static com.dariom.wds.api.v1.error.ErrorCode.DICTIONARY_EMPTY;
import static com.dariom.wds.domain.RoundPlayerStatus.PLAYING;
import static com.dariom.wds.websocket.model.EventType.ROUND_STARTED;
import static java.util.Comparator.naturalOrder;

import com.dariom.wds.config.WordleProperties;
import com.dariom.wds.domain.Language;
import com.dariom.wds.exception.InvalidGuessException;
import com.dariom.wds.persistence.entity.RoomEntity;
import com.dariom.wds.persistence.entity.RoundEntity;
import com.dariom.wds.persistence.repository.DictionaryRepository;
import com.dariom.wds.websocket.RoomEventPublisher;
import com.dariom.wds.websocket.model.RoomEvent;
import com.dariom.wds.websocket.model.RoundStartedPayload;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoundLifecycleService {

  private final DictionaryRepository dictionaryRepository;
  private final WordleProperties properties;
  private final RoomEventPublisher eventPublisher;

  public RoundEntity getCurrentRoundOrNull(RoomEntity room) {
    if (room.getCurrentRoundNumber() == null) {
      return null;
    }

    var currentRoundNumber = room.getCurrentRoundNumber();
    return room.getRounds().stream()
        .filter(round -> round.getRoundNumber() == currentRoundNumber)
        .findFirst()
        .orElse(null);
  }

  public RoundEntity startNewRound(RoomEntity room) {
    if (room.getPlayerIds().size() != 2) {
      throw new IllegalStateException("Cannot start round unless room has 2 players");
    }

    var language = room.getLanguage();
    var targetWord = randomTargetWord(language);
    var nextRoundNumber = room.getRounds().stream()
        .map(RoundEntity::getRoundNumber)
        .max(naturalOrder())
        .orElse(0) + 1;

    var round = new RoundEntity();
    round.setRoom(room);
    round.setRoundNumber(nextRoundNumber);
    round.setTargetWord(targetWord);
    round.setMaxAttempts(properties.maxAttempts());
    round.setFinished(false);
    round.setStartedAt(Instant.now());

    for (String playerId : room.getPlayerIds()) {
      round.setPlayerStatus(playerId, PLAYING);
    }

    room.addRound(round);
    room.setCurrentRoundNumber(nextRoundNumber);

    eventPublisher.publish(room.getId(), new RoomEvent(
        ROUND_STARTED,
        new RoundStartedPayload(round.getRoundNumber(), round.getMaxAttempts())
    ));

    return round;
  }

  private String randomTargetWord(Language language) {
    var answers = dictionaryRepository.getAnswerWords(language);
    if (answers.isEmpty()) {
      throw new InvalidGuessException(DICTIONARY_EMPTY,
          "No answer words available for language: %s".formatted(language));
    }

    var randomIndex = ThreadLocalRandom.current().nextInt(answers.size());
    return answers.stream().skip(randomIndex).findFirst().orElseThrow();
  }
}
