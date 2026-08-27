package com.dariom.wds.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RoomRounds {
  FIVE(5),
  TEN(10),
  ENDLESS(null);

  private final Integer count;

  @JsonCreator
  public static RoomRounds fromJson(JsonNode value) {
    if (value == null || value.isNull()) {
      throw new IllegalArgumentException("rounds must be 5, 10, or ENDLESS");
    }

    if (value.isIntegralNumber()) {
      return switch (value.intValue()) {
        case 5 -> FIVE;
        case 10 -> TEN;
        default -> throw new IllegalArgumentException("rounds must be 5, 10, or ENDLESS");
      };
    }

    if (value.isTextual() && value.textValue().equals("ENDLESS")) {
      return ENDLESS;
    }

    throw new IllegalArgumentException("rounds must be 5, 10, or ENDLESS");
  }

  @JsonValue
  public Object toJson() {
    return count == null ? name() : count;
  }

  public boolean isFinalRound(int roundNumber) {
    return count != null && roundNumber >= count;
  }
}
