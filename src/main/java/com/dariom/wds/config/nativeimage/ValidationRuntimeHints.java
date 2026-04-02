package com.dariom.wds.config.nativeimage;

import static org.springframework.aot.hint.MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS;

import com.dariom.wds.api.v1.validation.ValidLanguage;
import com.dariom.wds.api.v1.validation.ValidRoundNumber;
import com.dariom.wds.api.v1.validation.ValidWord;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

public class ValidationRuntimeHints implements RuntimeHintsRegistrar {

  @Override
  public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
    hints.reflection().registerType(ValidLanguage.Validator.class,
        INVOKE_PUBLIC_CONSTRUCTORS);
    hints.reflection().registerType(ValidWord.Validator.class,
        INVOKE_PUBLIC_CONSTRUCTORS);
    hints.reflection().registerType(ValidRoundNumber.Validator.class,
        INVOKE_PUBLIC_CONSTRUCTORS);
  }

}
