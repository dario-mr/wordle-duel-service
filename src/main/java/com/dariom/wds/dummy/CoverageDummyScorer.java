package com.dariom.wds.dummy;

import static java.util.Objects.requireNonNull;

public class CoverageDummyScorer {

  public int score(String input) {
    var value = requireNonNull(input, "input");

    var score = 0;
    for (var i = 0; i < value.length(); i++) {
      var c = value.charAt(i);
      if (Character.isDigit(c)) {
        score += c - '0';
      } else if (Character.isLetter(c)) {
        score += 1;
      } else if (Character.isWhitespace(c)) {
        score += 0;
      } else {
        score -= 1;
      }
    }

    return clamp(score, 0, 100);
  }

  private static int clamp(int value, int min, int max) {
    if (value < min) {
      return min;
    }

    if (value > max) {
      return max;
    }

    return value;
  }
}
