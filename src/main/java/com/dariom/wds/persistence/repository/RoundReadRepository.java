package com.dariom.wds.persistence.repository;

import static java.util.stream.Collectors.toCollection;

import com.dariom.wds.domain.Guess;
import com.dariom.wds.domain.LetterResult;
import com.dariom.wds.domain.Round;
import com.dariom.wds.domain.RoundPlayerStatus;
import com.dariom.wds.persistence.repository.jpa.RoundReadJpaRepository;
import com.dariom.wds.persistence.repository.jpa.projection.RoundHeaderView;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RoundReadRepository {

  private final RoundReadJpaRepository roundReadJpaRepository;

  public Optional<Round> findByRoomIdAndRoundNumber(String roomId, int roundNumber) {
    var headerOpt = roundReadJpaRepository.findRoundHeaderByRoomIdAndRoundNumber(roomId,
        roundNumber);
    return headerOpt.map(header -> buildRoundsByRoomId(List.of(header)).get(roomId));

  }

  public Map<String, Round> findCurrentByRoomIds(List<String> roomIds) {
    if (roomIds.isEmpty()) {
      return Map.of();
    }

    return buildRoundsByRoomId(roundReadJpaRepository.findCurrentRoundHeadersByRoomIds(roomIds));
  }

  private Map<String, Round> buildRoundsByRoomId(
      List<RoundHeaderView> headers
  ) {
    if (headers.isEmpty()) {
      return Map.of();
    }

    var roundIds = headers.stream()
        .map(RoundHeaderView::roundId)
        .collect(toCollection(LinkedHashSet::new));
    var statusesByRoundId = loadStatuses(roundIds);
    var guessesByRoundId = loadGuesses(roundIds);

    var roundsByRoomId = new LinkedHashMap<String, Round>();
    for (var header : headers) {
      roundsByRoomId.put(header.roomId(), new Round(
          header.roundNumber(),
          header.maxAttempts(),
          guessesByRoundId.getOrDefault(header.roundId(), Map.of()),
          statusesByRoundId.getOrDefault(header.roundId(), Map.of()),
          header.roundStatus(),
          header.targetWord()
      ));
    }

    return roundsByRoomId;
  }

  private Map<Long, Map<String, RoundPlayerStatus>> loadStatuses(LinkedHashSet<Long> roundIds) {
    var rows = roundReadJpaRepository.findRoundStatusesByRoundIds(roundIds);
    var statusesByRoundId = new LinkedHashMap<Long, Map<String, RoundPlayerStatus>>();
    for (var row : rows) {
      statusesByRoundId
          .computeIfAbsent(row.roundId(), ignored -> new LinkedHashMap<>())
          .put(row.playerId(), row.status());
    }
    return statusesByRoundId;
  }

  private Map<Long, Map<String, List<Guess>>> loadGuesses(LinkedHashSet<Long> roundIds) {
    var rows = roundReadJpaRepository.findGuessLettersByRoundIds(roundIds);
    var guessesByRoundId = new LinkedHashMap<Long, Map<String, List<Guess>>>();
    var buildersByGuessId = new LinkedHashMap<Long, GuessBuilder>();
    for (var row : rows) {
      var guessBuilder = buildersByGuessId.computeIfAbsent(row.guessId(), ignored -> {
        var builder = new GuessBuilder(row.word(), row.attemptNumber());
        guessesByRoundId
            .computeIfAbsent(row.roundId(), key -> new LinkedHashMap<>())
            .computeIfAbsent(row.playerId(), key -> new ArrayList<>())
            .add(builder.guess());
        return builder;
      });

      if (row.letterOrder() != null && row.letter() != null && row.letterStatus() != null) {
        guessBuilder.letters().add(new LetterResult(row.letter(), row.letterStatus()));
      }
    }

    return guessesByRoundId;
  }

  private static final class GuessBuilder {

    private final List<LetterResult> letters = new ArrayList<>();
    private final Guess guess;

    private GuessBuilder(String word, int attemptNumber) {
      guess = new Guess(word, letters, attemptNumber);
    }

    private List<LetterResult> letters() {
      return letters;
    }

    private Guess guess() {
      return guess;
    }
  }
}
