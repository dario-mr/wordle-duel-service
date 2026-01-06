package com.dariom.wds.dummy;

import static java.util.Objects.requireNonNull;

import java.util.Optional;

public class CoverageDummyCalculator {

  public Optional<Integer> parsePositiveInt(String raw) {
    if (raw == null) {
      return Optional.empty();
    }

    var trimmed = raw.trim();
    if (trimmed.isEmpty()) {
      return Optional.empty();
    }

    try {
      var value = Integer.parseInt(trimmed);
      if (value <= 0) {
        return Optional.empty();
      }

      return Optional.of(value);
    } catch (NumberFormatException e) {
      return Optional.empty();
    }
  }

  public int sumOrZero(String a, String b) {
    var first = parsePositiveInt(a).orElse(0);
    var second = parsePositiveInt(b).orElse(0);

    return first + second;
  }

  public String repeat(String value, int times) {
    var input = requireNonNull(value, "value");
    if (times <= 0) {
      return "";
    }

    var sb = new StringBuilder(input.length() * times);
    for (var i = 0; i < times; i++) {
      sb.append(input);
    }

    return sb.toString();
  }
}
