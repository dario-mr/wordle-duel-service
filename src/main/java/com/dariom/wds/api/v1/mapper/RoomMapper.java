package com.dariom.wds.api.v1.mapper;

import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import com.dariom.wds.api.v1.dto.GuessDto;
import com.dariom.wds.api.v1.dto.LetterResultDto;
import com.dariom.wds.api.v1.dto.RoomDto;
import com.dariom.wds.api.v1.dto.RoundDto;
import com.dariom.wds.domain.Room;
import com.dariom.wds.persistence.entity.GuessEntity;
import com.dariom.wds.persistence.entity.RoundEntity;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Component;

@Component
public class RoomMapper {

  public RoomDto toDto(Room room) {
    var roomEntity = room.room();
    return new RoomDto(
        roomEntity.getId(),
        roomEntity.getLanguage(),
        roomEntity.getStatus(),
        roomEntity.getSortedPlayerIds(),
        new TreeMap<>(roomEntity.getScoresByPlayerId()),
        toRoundDto(room.currentRound())
    );
  }

  private RoundDto toRoundDto(RoundEntity round) {
    if (round == null) {
      return null;
    }

    Map<String, List<GuessDto>> guessesByPlayerId = round.getGuesses().stream()
        .sorted(comparingInt(GuessEntity::getAttemptNumber))
        .collect(groupingBy(
            GuessEntity::getPlayerId,
            mapping(this::toGuessDto, toList())
        ));

    return new RoundDto(
        round.getRoundNumber(),
        round.getMaxAttempts(),
        guessesByPlayerId,
        round.getStatusByPlayerId().entrySet().stream()
            .collect(toMap(Map.Entry::getKey, e -> e.getValue().name())),
        round.isFinished()
    );
  }

  private GuessDto toGuessDto(GuessEntity guess) {
    var letters = guess.getLetters().stream()
        .map(l -> new LetterResultDto(l.getLetter(), l.getStatus()))
        .toList();

    return new GuessDto(
        guess.getWord(),
        letters,
        guess.getAttemptNumber()
    );
  }

}
