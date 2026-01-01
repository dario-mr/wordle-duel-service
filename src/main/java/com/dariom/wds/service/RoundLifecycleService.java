package com.dariom.wds.service;

import static com.dariom.wds.api.v1.error.ErrorCode.DICTIONARY_EMPTY;

import com.dariom.wds.config.WordleProperties;
import com.dariom.wds.domain.Language;
import com.dariom.wds.domain.RoundPlayerStatus;
import com.dariom.wds.exception.InvalidGuessException;
import com.dariom.wds.persistence.entity.RoomEntity;
import com.dariom.wds.persistence.entity.RoundEntity;
import com.dariom.wds.persistence.repository.DictionaryRepository;
import com.dariom.wds.websocket.RoomEvent;
import com.dariom.wds.websocket.RoomEventPublisher;
import com.dariom.wds.websocket.RoundStartedPayload;
import java.time.Instant;
import java.util.Comparator;
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

    int current = room.getCurrentRoundNumber();
    return room.getRounds().stream()
        .filter(r -> r.getRoundNumber() == current)
        .findFirst()
        .orElse(null);
  }

  public RoundEntity startNewRound(RoomEntity room) {
    if (room.getPlayerIds().size() != 2) {
      throw new IllegalStateException("Cannot start round unless room has 2 players");
    }

    var language = room.getLanguage();
    var targetWord = randomTargetWord(language);

    int nextRoundNumber = room.getRounds().stream()
        .map(RoundEntity::getRoundNumber)
        .max(Comparator.naturalOrder())
        .orElse(0) + 1;

    var round = new RoundEntity();
    round.setRoom(room);
    round.setRoundNumber(nextRoundNumber);
    round.setTargetWord(targetWord);
    round.setMaxAttempts(properties.maxAttempts());
    round.setFinished(false);
    round.setStartedAt(Instant.now());

    for (String playerId : room.getPlayerIds()) {
      round.getStatusByPlayerId().put(playerId, RoundPlayerStatus.PLAYING);
    }

    room.getRounds().add(round);
    room.setCurrentRoundNumber(nextRoundNumber);

    eventPublisher.publish(room.getId(), new RoomEvent(
        "ROUND_STARTED",
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

    int idx = ThreadLocalRandom.current().nextInt(answers.size());
    return answers.stream().skip(idx).findFirst().orElseThrow();
  }
}
