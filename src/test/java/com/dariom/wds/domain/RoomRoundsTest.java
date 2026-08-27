package com.dariom.wds.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RoomRoundsTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  static Stream<Arguments> jsonValues() {
    return Stream.of(
        Arguments.of(RoomRounds.FIVE, "5"),
        Arguments.of(RoomRounds.TEN, "10"),
        Arguments.of(RoomRounds.ENDLESS, "\"ENDLESS\"")
    );
  }

  @ParameterizedTest
  @MethodSource("jsonValues")
  void jsonValue_roundTrip_preservesContractValue(RoomRounds rounds, String json) throws Exception {
    // Act
    var serialized = objectMapper.writeValueAsString(rounds);
    var deserialized = objectMapper.readValue(serialized, RoomRounds.class);

    // Assert
    assertThat(serialized).isEqualTo(json);
    assertThat(deserialized).isEqualTo(rounds);
  }
}
