package com.dariom.wds.api.v1.mapper;

import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.toMap;

import com.dariom.wds.api.v1.dto.GuessDto;
import com.dariom.wds.api.v1.dto.LetterResultDto;
import com.dariom.wds.api.v1.dto.RoomDto;
import com.dariom.wds.api.v1.dto.RoundDto;
import com.dariom.wds.domain.Guess;
import com.dariom.wds.domain.Room;
import com.dariom.wds.domain.Round;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Component;

@Component
public class RoomMapper {

  public RoomDto toDto(Room room) {
    return new RoomDto(
        room.id(),
        room.language(),
        room.status(),
        room.players(),
        new TreeMap<>(room.scoresByPlayerId()),
        toRoundDto(room.currentRound())
    );
  }

  private RoundDto toRoundDto(Round round) {
    if (round == null) {
      return null;
    }

    var guessesByPlayerId = round.guessesByPlayerId().entrySet().stream()
        .collect(toMap(
            Map.Entry::getKey,
            e -> e.getValue().stream()
                .sorted(comparingInt(Guess::attemptNumber))
                .map(this::toGuessDto)
                .toList()
        ));

    return new RoundDto(
        round.roundNumber(),
        round.maxAttempts(),
        guessesByPlayerId,
        round.statusByPlayerId().entrySet().stream()
            .collect(toMap(Map.Entry::getKey, e -> e.getValue().name())),
        round.finished()
    );
  }

  private GuessDto toGuessDto(Guess guess) {
    var letters = guess.letters().stream()
        .map(l -> new LetterResultDto(l.letter(), l.status()))
        .toList();

    return new GuessDto(
        guess.word(),
        letters,
        guess.attemptNumber()
    );
  }

}
