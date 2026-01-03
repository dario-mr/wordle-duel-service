package com.dariom.wds.service;

import static com.dariom.wds.domain.LetterStatus.ABSENT;
import static com.dariom.wds.domain.LetterStatus.CORRECT;
import static com.dariom.wds.domain.LetterStatus.PRESENT;
import static org.assertj.core.api.Assertions.assertThat;

import com.dariom.wds.domain.LetterResult;
import com.dariom.wds.domain.LetterStatus;
import java.util.List;
import org.junit.jupiter.api.Test;

class WordleEvaluatorTest {

  private final WordleEvaluator evaluator = new WordleEvaluator();

  @Test
  void evaluate_guessEqualsTarget_returnsAllCorrect() {
    // Arrange
    var target = "PIZZA";
    var guess = "PIZZA";

    // Act
    List<LetterResult> results = evaluator.evaluate(target, guess);

    // Assert
    assertThat(results).hasSize(5);
    assertStatuses(results, CORRECT, CORRECT, CORRECT, CORRECT, CORRECT);
  }

  @Test
  void evaluate_mixedCaseInput_uppercasesWords() {
    // Arrange
    var target = "pizza";
    var guess = "pIzZa";

    // Act
    List<LetterResult> results = evaluator.evaluate(target, guess);

    // Assert
    assertThat(results).hasSize(5);
    assertStatuses(results, CORRECT, CORRECT, CORRECT, CORRECT, CORRECT);
    assertThat(results).extracting(LetterResult::letter).containsExactly('P', 'I', 'Z', 'Z', 'A');
  }

  @Test
  void evaluate_duplicateLettersAcrossTargetAndGuess_respectsCounts() {
    // Arrange
    var target = "MAMMA";
    var guess = "AMMMA";

    // Act
    List<LetterResult> results = evaluator.evaluate(target, guess);

    // Assert
    assertThat(results).hasSize(5);
    assertStatuses(results, PRESENT, PRESENT, CORRECT, CORRECT, CORRECT);
  }

  @Test
  void evaluate_guessHasExcessDuplicates_respectsCounts() {
    // Arrange
    var target = "PIZZA";
    var guess = "ZZZZZ";

    // Act
    List<LetterResult> results = evaluator.evaluate(target, guess);

    // Assert
    assertThat(results).hasSize(5);
    assertStatuses(results, ABSENT, ABSENT, CORRECT, CORRECT, ABSENT);
  }

  @Test
  void evaluate_guessHasExtraDuplicates_doesNotMarkExtrasPresent() {
    // Arrange
    var target = "ABCDE";
    var guess = "EAAAA";

    // Act
    List<LetterResult> results = evaluator.evaluate(target, guess);

    // Assert
    assertThat(results).hasSize(5);
    assertStatuses(results, PRESENT, PRESENT, ABSENT, ABSENT, ABSENT);
  }

  @Test
  void evaluate_correctAndPresentMatches_prioritizesCorrect() {
    // Arrange
    var target = "BALSA";
    var guess = "AAAAA";

    // Act
    List<LetterResult> results = evaluator.evaluate(target, guess);

    // Assert
    assertThat(results).hasSize(5);
    assertStatuses(results, ABSENT, CORRECT, ABSENT, ABSENT, CORRECT);
  }

  @Test
  void evaluate_guessHasMoreOccurrencesThanTarget_limitsPresentMatches() {
    // Arrange
    var target = "AABCD";
    var guess = "EEAAA";

    // Act
    List<LetterResult> results = evaluator.evaluate(target, guess);

    // Assert
    assertThat(results).hasSize(5);
    assertStatuses(results, ABSENT, ABSENT, PRESENT, PRESENT, ABSENT);
  }

  private static void assertStatuses(List<LetterResult> results, LetterStatus... expected) {
    assertThat(results)
        .extracting(LetterResult::status)
        .containsExactly(expected);
  }
}
