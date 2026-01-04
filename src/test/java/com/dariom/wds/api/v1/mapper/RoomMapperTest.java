package com.dariom.wds.api.v1.mapper;

import static com.dariom.wds.domain.Language.IT;
import static com.dariom.wds.domain.LetterStatus.CORRECT;
import static com.dariom.wds.domain.RoomStatus.IN_PROGRESS;
import static org.assertj.core.api.Assertions.assertThat;

import com.dariom.wds.api.v1.dto.GuessDto;
import com.dariom.wds.domain.Guess;
import com.dariom.wds.domain.LetterResult;
import com.dariom.wds.domain.Room;
import com.dariom.wds.domain.Round;
import com.dariom.wds.domain.RoundPlayerStatus;
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
        false
    );

    var room = new Room("room-1", IT, IN_PROGRESS,
        List.of("p1"), Map.of("p1", 0),
        round
    );

    // Act
    var dto = mapper.toDto(room);

    // Assert
    assertThat(dto.currentRound()).isNotNull();
    assertThat(dto.currentRound().guessesByPlayerId()).containsKey("p1");
    assertThat(dto.currentRound().guessesByPlayerId().get("p1"))
        .extracting(GuessDto::attemptNumber)
        .containsExactly(1, 2);
    assertThat(dto.scoresByPlayerId().keySet()).containsExactly("p1");
  }
}
