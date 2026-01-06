package com.dariom.wds.dummy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CoverageDummyCalculatorTest {

  private final CoverageDummyCalculator calculator = new CoverageDummyCalculator();

  @Test
  void parsePositiveInt_null_returnsEmpty() {
    // Arrange
    String input = null;

    // Act
    var result = calculator.parsePositiveInt(input);

    // Assert
    assertThat(result).isEmpty();
  }

  @Test
  void parsePositiveInt_blank_returnsEmpty() {
    // Arrange
    var input = "  ";

    // Act
    var result = calculator.parsePositiveInt(input);

    // Assert
    assertThat(result).isEmpty();
  }

  @Test
  void parsePositiveInt_nonNumeric_returnsEmpty() {
    // Arrange
    var input = "abc";

    // Act
    var result = calculator.parsePositiveInt(input);

    // Assert
    assertThat(result).isEmpty();
  }

  @Test
  void parsePositiveInt_zeroOrNegative_returnsEmpty() {
    // Arrange
    var zero = "0";
    var negative = "-1";

    // Act
    var zeroResult = calculator.parsePositiveInt(zero);
    var negativeResult = calculator.parsePositiveInt(negative);

    // Assert
    assertThat(zeroResult).isEmpty();
    assertThat(negativeResult).isEmpty();
  }

  @Test
  void parsePositiveInt_positiveWithSpaces_returnsValue() {
    // Arrange
    var input = "  42 ";

    // Act
    var result = calculator.parsePositiveInt(input);

    // Assert
    assertThat(result).contains(42);
  }

  @Test
  void sumOrZero_missingValues_returnsZero() {
    // Arrange
    var first = " ";
    String second = null;

    // Act
    var result = calculator.sumOrZero(first, second);

    // Assert
    assertThat(result).isZero();
  }

  @Test
  void sumOrZero_validValues_returnsSum() {
    // Arrange
    var first = "7";
    var second = " 3 ";

    // Act
    var result = calculator.sumOrZero(first, second);

    // Assert
    assertThat(result).isEqualTo(10);
  }

  @Test
  void repeat_nonPositiveTimes_returnsEmptyString() {
    // Arrange
    var value = "ab";

    // Act
    var result = calculator.repeat(value, 0);

    // Assert
    assertThat(result).isEmpty();
  }

  @Test
  void repeat_positiveTimes_returnsRepeatedValue() {
    // Arrange
    var value = "ab";

    // Act
    var result = calculator.repeat(value, 3);

    // Assert
    assertThat(result).isEqualTo("ababab");
  }
}
