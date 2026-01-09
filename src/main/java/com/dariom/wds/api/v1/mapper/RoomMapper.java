package com.dariom.wds.api.v1.mapper;

import static java.util.Collections.emptyList;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.toMap;

import com.dariom.wds.api.v1.dto.GuessDto;
import com.dariom.wds.api.v1.dto.LetterResultDto;
import com.dariom.wds.api.v1.dto.PlayerDto;
import com.dariom.wds.api.v1.dto.RoomDto;
import com.dariom.wds.api.v1.dto.RoundDto;
import com.dariom.wds.domain.Guess;
import com.dariom.wds.domain.Player;
import com.dariom.wds.domain.Room;
import com.dariom.wds.domain.Round;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class RoomMapper {

  public RoomDto toDto(Room room) {
    return new RoomDto(
        room.id(),
        room.language(),
        room.status(),
        toPlayerDto(room.players()),
        toRoundDto(room.currentRound())
    );
  }

  private List<PlayerDto> toPlayerDto(List<Player> players) {
    if (players == null) {
      return emptyList();
    }

    return players.stream()
        .map(p -> new PlayerDto(p.id(), p.score()))
        .toList();
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
        round.roundStatus(),
        round.solution()
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
