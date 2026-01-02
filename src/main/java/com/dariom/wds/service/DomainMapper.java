package com.dariom.wds.service;

import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import com.dariom.wds.domain.Guess;
import com.dariom.wds.domain.LetterResult;
import com.dariom.wds.domain.Room;
import com.dariom.wds.domain.Round;
import com.dariom.wds.persistence.entity.GuessEntity;
import com.dariom.wds.persistence.entity.RoomEntity;
import com.dariom.wds.persistence.entity.RoundEntity;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Component;

@Component
public class DomainMapper {

  public Room toRoom(RoomEntity room, Round currentRound) {
    return new Room(
        room.getId(),
        room.getLanguage(),
        room.getStatus(),
        room.getSortedPlayerIds(),
        new TreeMap<>(room.getScoresByPlayerId()),
        currentRound
    );
  }

  public Round toRound(RoundEntity round) {
    if (round == null) {
      return null;
    }

    var guessesByPlayerId = round.getGuesses().stream()
        .sorted(comparingInt(GuessEntity::getAttemptNumber))
        .collect(groupingBy(
            GuessEntity::getPlayerId,
            mapping(this::toGuess, toList())
        ));

    return new Round(
        round.getRoundNumber(),
        round.getMaxAttempts(),
        guessesByPlayerId,
        Map.copyOf(round.getStatusByPlayerId()),
        round.isFinished()
    );
  }

  private Guess toGuess(GuessEntity guess) {
    var letters = guess.getLetters().stream()
        .map(l -> new LetterResult(l.getLetter(), l.getStatus()))
        .toList();

    return new Guess(
        guess.getWord(),
        letters,
        guess.getAttemptNumber()
    );
  }
}
