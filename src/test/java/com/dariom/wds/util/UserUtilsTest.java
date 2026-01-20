package com.dariom.wds.util;

import static com.dariom.wds.util.UserUtils.normalizeFullName;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class UserUtilsTest {

  private static final String ANONYMOUS = "Anonymous";

  @ParameterizedTest
  @MethodSource("normalizeFullNameCases")
  void normalizeFullName_returnsExpectedFirstName(String input, String expected) {
    assertThat(normalizeFullName(input)).isEqualTo(expected);
  }

  static Stream<Arguments> normalizeFullNameCases() {
    return Stream.of(
        // null / blank
        Arguments.of(null, ANONYMOUS),
        Arguments.of("", ANONYMOUS),
        Arguments.of("   ", ANONYMOUS),
        Arguments.of("\t\n\r", ANONYMOUS),

        // basic shapes
        Arguments.of("John Doe", "John"),
        Arguments.of("John", "John"),
        Arguments.of("John       Doe", "John"),
        Arguments.of("John Doe The Second", "John"),

        // leading/trailing whitespace
        Arguments.of("   John Doe   ", "John"),
        Arguments.of("   John   ", "John"),

        // only first token taken even if many spaces
        Arguments.of("John     Doe     The     Second", "John"),

        // max-length truncation (32)
        Arguments.of("ExtremelyLongNameThatShouldBeCutAtThirtyTwoChars Doe",
            "ExtremelyLongNameThatShouldBeCut"),
        // exactly 32 chars stays as-is
        Arguments.of("12345678901234567890123456789012 Doe", "12345678901234567890123456789012"),
        // shorter than 32 unchanged
        Arguments.of("ShortName Doe", "ShortName")
    );
  }

}