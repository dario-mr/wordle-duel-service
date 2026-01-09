package com.dariom.wds.service;

import static com.dariom.wds.domain.RoundStatus.ENDED;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import com.dariom.wds.domain.Guess;
import com.dariom.wds.domain.LetterResult;
import com.dariom.wds.domain.Player;
import com.dariom.wds.domain.Room;
import com.dariom.wds.domain.Round;
import com.dariom.wds.persistence.entity.GuessEntity;
import com.dariom.wds.persistence.entity.RoomEntity;
import com.dariom.wds.persistence.entity.RoomPlayerEntity;
import com.dariom.wds.persistence.entity.RoundEntity;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class DomainMapper {

  public Room toRoom(RoomEntity room, Round currentRound) {
    return new Room(
        room.getId(),
        room.getLanguage(),
        room.getStatus(),
        toPlayers(room.getRoomPlayers()),
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

    var solution = round.getRoundStatus() == ENDED ? round.getTargetWord() : null;

    return new Round(
        round.getRoundNumber(),
        round.getMaxAttempts(),
        guessesByPlayerId,
        Map.copyOf(round.getStatusByPlayerId()),
        round.getRoundStatus(),
        solution
    );
  }

  private List<Player> toPlayers(Set<RoomPlayerEntity> roomPlayers) {
    if (roomPlayers == null) {
      return emptyList();
    }

    return roomPlayers.stream()
        .sorted(comparing(RoomPlayerEntity::getPlayerId))
        .map(p -> new Player(p.getPlayerId(), p.getScore()))
        .toList();
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
