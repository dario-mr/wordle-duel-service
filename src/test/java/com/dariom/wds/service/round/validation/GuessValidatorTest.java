package com.dariom.wds.service.round.validation;

import static com.dariom.wds.api.v1.error.ErrorCode.INVALID_CHARS;
import static com.dariom.wds.api.v1.error.ErrorCode.INVALID_LANGUAGE;
import static com.dariom.wds.api.v1.error.ErrorCode.INVALID_LENGTH;
import static com.dariom.wds.api.v1.error.ErrorCode.WORD_NOT_ALLOWED;
import static com.dariom.wds.domain.Language.IT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dariom.wds.config.WordleProperties;
import com.dariom.wds.exception.InvalidGuessException;
import com.dariom.wds.persistence.repository.DictionaryRepository;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GuessValidatorTest {

  @Mock
  private DictionaryRepository dictionaryRepository;

  private final WordleProperties properties = new WordleProperties(6, 5);

  private GuessValidator validator;

  @BeforeEach
  void setUp() {
    validator = new GuessValidator(properties, dictionaryRepository);
  }

  @Test
  void validateGuess_targetLengthMismatch_throwsInvalidLength() {
    // Act
    var thrown = catchThrowable(() -> validator.validateGuess("PIZZ", "PIZZA", IT));

    // Assert
    assertThat(thrown)
        .isInstanceOfSatisfying(
            InvalidGuessException.class,
            ex -> assertThat(ex.getCode()).isEqualTo(INVALID_LENGTH));
  }

  @Test
  void validateGuess_guessLengthDoesNotMatchConfiguredWordLength_throwsInvalidLength() {
    // Act
    var thrown = catchThrowable(() -> validator.validateGuess("PIZZAA", "PIZZAA", IT));

    // Assert
    assertThat(thrown)
        .isInstanceOfSatisfying(
            InvalidGuessException.class,
            ex -> assertThat(ex.getCode()).isEqualTo(INVALID_LENGTH));
  }

  @Test
  void validateGuess_guessContainsNonLetters_throwsInvalidChars() {
    // Act
    var thrown = catchThrowable(() -> validator.validateGuess("PI2ZA", "PIZZA", IT));

    // Assert
    assertThat(thrown)
        .isInstanceOfSatisfying(
            InvalidGuessException.class,
            ex -> assertThat(ex.getCode()).isEqualTo(INVALID_CHARS));
  }

  @Test
  void validateGuess_languageNull_throwsInvalidLanguage() {
    // Act
    var thrown = catchThrowable(() -> validator.validateGuess("PIZZA", "PIZZA", null));

    // Assert
    assertThat(thrown)
        .isInstanceOfSatisfying(
            InvalidGuessException.class,
            ex -> assertThat(ex.getCode()).isEqualTo(INVALID_LANGUAGE));
  }

  @Test
  void validateGuess_notInAllowedDictionary_throwsWordNotAllowed() {
    // Arrange
    when(dictionaryRepository.getAllowedGuesses(any())).thenReturn(Set.of("PASTA"));

    // Act
    var thrown = catchThrowable(() -> validator.validateGuess("PIZZA", "PIZZA", IT));

    // Assert
    assertThat(thrown)
        .isInstanceOfSatisfying(
            InvalidGuessException.class,
            ex -> assertThat(ex.getCode()).isEqualTo(WORD_NOT_ALLOWED));

    verify(dictionaryRepository).getAllowedGuesses(IT);
  }

  @Test
  void validateGuess_validInput_doesNotThrow() {
    // Arrange
    when(dictionaryRepository.getAllowedGuesses(any())).thenReturn(Set.of("PIZZA"));

    // Act
    validator.validateGuess("PIZZA", "PIZZA", IT);

    // Assert
    verify(dictionaryRepository).getAllowedGuesses(IT);
  }
}
