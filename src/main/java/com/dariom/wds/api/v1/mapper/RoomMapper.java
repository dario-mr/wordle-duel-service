package com.dariom.wds.api.v1.mapper;

import static com.dariom.wds.domain.RoundPlayerStatus.PLAYING;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparingInt;

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
import org.springframework.stereotype.Component;

@Component
public class RoomMapper {

  public RoomDto toDto(Room room, String requestingPlayerId) {
    return new RoomDto(
        room.id(),
        room.language(),
        room.rounds(),
        room.status(),
        toPlayerDto(room.players()),
        toRoundDto(room.currentRound(), requestingPlayerId)
    );
  }

  private List<PlayerDto> toPlayerDto(List<Player> players) {
    if (players == null) {
      return emptyList();
    }

    return players.stream()
        .map(p -> new PlayerDto(p.id(), p.score(), p.displayName()))
        .toList();
  }

  private RoundDto toRoundDto(Round round, String requestingPlayerId) {
    if (round == null) {
      return null;
    }

    var solution = shouldRevealSolution(round, requestingPlayerId)
        ? round.solution() : null;

    return new RoundDto(
        round.roundNumber(),
        round.maxAttempts(),
        round.guesses().stream()
            .sorted(comparingInt(Guess::attemptNumber))
            .map(this::toGuessDto)
            .toList(),
        round.playerStatus() == null ? null : round.playerStatus().name(),
        round.roundStatus(),
        solution
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

  private static boolean shouldRevealSolution(Round round, String requestingPlayerId) {
    if (requestingPlayerId == null) {
      return false;
    }

    return round.playerStatus() != null && round.playerStatus() != PLAYING;
  }

}
