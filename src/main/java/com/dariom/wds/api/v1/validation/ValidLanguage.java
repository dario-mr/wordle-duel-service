package com.dariom.wds.api.v1.validation;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.dariom.wds.domain.Language;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = ValidLanguage.Validator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidLanguage {

  String message() default "language is required";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  class Validator implements ConstraintValidator<ValidLanguage, String> {

    @Override
    public boolean isValid(String language, ConstraintValidatorContext context) {
      if (!isNotBlank(language)) {
        return false;
      }

      try {
        Language.valueOf(language.trim().toUpperCase());
        return true;
      } catch (Exception ex) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate("language is invalid")
            .addConstraintViolation();
        return false;
      }
    }
  }
}
