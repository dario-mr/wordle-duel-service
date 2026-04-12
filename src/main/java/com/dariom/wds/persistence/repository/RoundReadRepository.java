package com.dariom.wds.persistence.repository;

import com.dariom.wds.domain.Guess;
import com.dariom.wds.domain.LetterResult;
import com.dariom.wds.domain.Round;
import com.dariom.wds.domain.RoundPlayerStatus;
import com.dariom.wds.persistence.repository.jpa.RoundReadJpaRepository;
import com.dariom.wds.persistence.repository.jpa.projection.RoundFlatRowView;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
    var roundRows = roundReadJpaRepository.findFlatRowsByRoomIdAndRoundNumber(roomId, roundNumber);
    var round = buildRoundsByRoomId(roundRows).get(roomId);
    return Optional.ofNullable(round);
  }

  public Map<String, Round> findCurrentByRoomIds(List<String> roomIds) {
    if (roomIds.isEmpty()) {
      return Map.of();
    }

    return buildRoundsByRoomId(roundReadJpaRepository.findCurrentFlatRowsByRoomIds(roomIds));
  }

  private Map<String, Round> buildRoundsByRoomId(List<RoundFlatRowView> rows) {
    if (rows.isEmpty()) {
      return Map.of();
    }

    var buildersByRoundId = new LinkedHashMap<Long, RoundBuilder>();
    for (var row : rows) {
      var builder = buildersByRoundId.computeIfAbsent(row.roundId(), ignored -> new RoundBuilder(
          row.roomId(),
          row.roundNumber(),
          row.maxAttempts(),
          row.roundStatus(),
          row.targetWord()
      ));
      builder.addStatus(row);
      builder.addGuess(row);
    }

    var roundsByRoomId = new LinkedHashMap<String, Round>();
    for (var builder : buildersByRoundId.values()) {
      roundsByRoomId.put(builder.roomId(), builder.build());
    }
    return roundsByRoomId;
  }

  private static final class GuessBuilder {

    private final List<LetterResult> letters = new ArrayList<>();
    private final List<Integer> seenLetterOrders = new ArrayList<>();
    private final Guess guess;

    private GuessBuilder(String word, int attemptNumber) {
      guess = new Guess(word, letters, attemptNumber);
    }

    private void addLetterIfAbsent(int letterOrder, LetterResult letter) {
      if (seenLetterOrders.contains(letterOrder)) {
        return;
      }
      seenLetterOrders.add(letterOrder);
      letters.add(letter);
    }

    private Guess guess() {
      return guess;
    }
  }

  private static final class RoundBuilder {

    private final String roomId;
    private final int roundNumber;
    private final int maxAttempts;
    private final Map<String, List<Guess>> guessesByPlayerId = new LinkedHashMap<>();
    private final Map<String, RoundPlayerStatus> statusByPlayerId = new LinkedHashMap<>();
    private final com.dariom.wds.domain.RoundStatus roundStatus;
    private final String targetWord;
    private final Map<Long, GuessBuilder> buildersByGuessId = new LinkedHashMap<>();

    private RoundBuilder(
        String roomId,
        int roundNumber,
        int maxAttempts,
        com.dariom.wds.domain.RoundStatus roundStatus,
        String targetWord
    ) {
      this.roomId = roomId;
      this.roundNumber = roundNumber;
      this.maxAttempts = maxAttempts;
      this.roundStatus = roundStatus;
      this.targetWord = targetWord;
    }

    private String roomId() {
      return roomId;
    }

    private void addStatus(RoundFlatRowView row) {
      if (row.statusPlayerId() != null && row.playerStatus() != null) {
        statusByPlayerId.put(row.statusPlayerId(), row.playerStatus());
      }
    }

    private void addGuess(RoundFlatRowView row) {
      if (row.guessId() < 0 || row.guessPlayerId() == null || row.guessWord() == null
          || row.attemptNumber() == null) {
        return;
      }

      var guessBuilder = buildersByGuessId.computeIfAbsent(row.guessId(), ignored -> {
        var builder = new GuessBuilder(row.guessWord(), row.attemptNumber());
        guessesByPlayerId
            .computeIfAbsent(row.guessPlayerId(), _ -> new ArrayList<>())
            .add(builder.guess());
        return builder;
      });

      if (row.letterOrder() != null && row.letter() != null && row.letterStatus() != null) {
        guessBuilder.addLetterIfAbsent(row.letterOrder(),
            new LetterResult(row.letter(), row.letterStatus()));
      }
    }

    private Round build() {
      return new Round(roundNumber, maxAttempts, guessesByPlayerId, statusByPlayerId, roundStatus,
          targetWord);
    }
  }
}
