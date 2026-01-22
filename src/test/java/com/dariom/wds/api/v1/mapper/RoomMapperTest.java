package com.dariom.wds.api.v1.mapper;

import static com.dariom.wds.domain.Language.IT;
import static com.dariom.wds.domain.LetterStatus.CORRECT;
import static com.dariom.wds.domain.RoomStatus.IN_PROGRESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.dariom.wds.api.v1.dto.GuessDto;
import com.dariom.wds.api.v1.dto.PlayerDto;
import com.dariom.wds.domain.Guess;
import com.dariom.wds.domain.LetterResult;
import com.dariom.wds.domain.Player;
import com.dariom.wds.domain.Room;
import com.dariom.wds.domain.Round;
import com.dariom.wds.domain.RoundPlayerStatus;
import com.dariom.wds.domain.RoundStatus;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RoomMapperTest {

  private final RoomMapper mapper = new RoomMapper();

  @Test
  void toDto_unsortedGuesses_returnsSortedGuessesByAttemptNumber() {
    // Arrange
    var guess2 = new Guess("PIZZA", List.of(new LetterResult('P', CORRECT)), 2);
    var guess1 = new Guess("PASTA", List.of(new LetterResult('P', CORRECT)), 1);

    var round = new Round(1, 6,
        Map.of("p1", List.of(guess2, guess1)),
        Map.of("p1", RoundPlayerStatus.PLAYING),
        RoundStatus.PLAYING,
        null
    );

    var room = new Room("room-1", IT, IN_PROGRESS, List.of(new Player("p1", 0, "John")), round);

    // Act
    var dto = mapper.toDto(room, "p1");

    // Assert
    assertThat(dto.currentRound()).isNotNull();
    assertThat(dto.currentRound().guessesByPlayerId()).containsKey("p1");
    assertThat(dto.currentRound().guessesByPlayerId().get("p1"))
        .extracting(GuessDto::attemptNumber)
        .containsExactly(1, 2);
    assertThat(dto.players())
        .extracting(PlayerDto::id, PlayerDto::score, PlayerDto::displayName)
        .containsExactly(tuple("p1", 0, "John"));
  }

  @Test
  void toDto_roundPlayingAndRequesterNotLost_hidesSolution() {
    // Arrange
    var round = new Round(1, 6,
        Map.of("p1", List.of()),
        Map.of("p1", RoundPlayerStatus.PLAYING),
        RoundStatus.PLAYING,
        "PIZZA"
    );

    var room = new Room("room-1", IT, IN_PROGRESS, List.of(new Player("p1", 0, "John")), round);

    // Act
    var dto = mapper.toDto(room, "p1");

    // Assert
    assertThat(dto.currentRound().solution()).isNull();
  }

  @Test
  void toDto_roundPlayingAndRequesterLost_revealsSolution() {
    // Arrange
    var round = new Round(1, 6,
        Map.of("p1", List.of()),
        Map.of("p1", RoundPlayerStatus.LOST),
        RoundStatus.PLAYING,
        "PIZZA"
    );

    var room = new Room("room-1", IT, IN_PROGRESS, List.of(new Player("p1", 0, "John")), round);

    // Act
    var dto = mapper.toDto(room, "p1");

    // Assert
    assertThat(dto.currentRound().solution()).isEqualTo("PIZZA");
  }

  @Test
  void toDto_roundEnded_revealsSolutionToAnyRequester() {
    // Arrange
    var round = new Round(1, 6,
        Map.of("p1", List.of()),
        Map.of("p1", RoundPlayerStatus.WON),
        RoundStatus.ENDED,
        "PIZZA"
    );

    var room = new Room("room-1", IT, IN_PROGRESS, List.of(new Player("p1", 0, "John")), round);

    // Act
    var dto = mapper.toDto(room, "someone-else");

    // Assert
    assertThat(dto.currentRound().solution()).isEqualTo("PIZZA");
  }
}
