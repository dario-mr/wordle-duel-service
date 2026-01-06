package com.dariom.wds.api.v1.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApiValidationTest {

  @Mock
  private ConstraintValidatorContext context;

  @Mock
  private ConstraintViolationBuilder violationBuilder;

  @Test
  void isValid_nullWord_returnsFalse() {
    // Arrange
    var validator = new ValidWord.Validator();

    // Act
    var result = validator.isValid(null, context);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  void isValid_blankWord_returnsFalse() {
    // Arrange
    var validator = new ValidWord.Validator();

    // Act
    var result = validator.isValid(" ", context);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  void isValid_nonBlankWord_returnsTrue() {
    // Arrange
    var validator = new ValidWord.Validator();

    // Act
    var result = validator.isValid("pizza", context);

    // Assert
    assertThat(result).isTrue();
  }

  @Test
  void isValid_nullPlayerId_returnsFalse() {
    // Arrange
    var validator = new ValidPlayerId.Validator();

    // Act
    var result = validator.isValid(null, context);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  void isValid_blankPlayerId_returnsFalse() {
    // Arrange
    var validator = new ValidPlayerId.Validator();

    // Act
    var result = validator.isValid(" ", context);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  void isValid_nonBlankPlayerId_returnsTrue() {
    // Arrange
    var validator = new ValidPlayerId.Validator();

    // Act
    var result = validator.isValid("p1", context);

    // Assert
    assertThat(result).isTrue();
  }

  @Test
  void isValid_validLanguage_returnsTrue() {
    // Arrange
    var validator = new ValidLanguage.Validator();

    // Act
    var validLowercase = validator.isValid("it", context);
    var validWithWhitespace = validator.isValid(" IT ", context);

    // Assert
    assertThat(validLowercase).isTrue();
    assertThat(validWithWhitespace).isTrue();
  }

  @Test
  void isValid_invalidLanguage_returnsFalseAndOverridesMessage() {
    // Arrange
    var validator = new ValidLanguage.Validator();
    when(context.buildConstraintViolationWithTemplate("language is invalid"))
        .thenReturn(violationBuilder);

    // Act
    var result = validator.isValid("xx", context);

    // Assert
    assertThat(result).isFalse();
    verify(context).disableDefaultConstraintViolation();
    verify(context).buildConstraintViolationWithTemplate("language is invalid");
    verify(violationBuilder).addConstraintViolation();
  }
}
