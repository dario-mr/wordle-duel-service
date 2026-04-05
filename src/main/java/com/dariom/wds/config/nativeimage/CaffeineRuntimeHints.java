package com.dariom.wds.config.nativeimage;

import static org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_CONSTRUCTORS;
import static org.springframework.aot.hint.MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;

public class CaffeineRuntimeHints implements RuntimeHintsRegistrar {

  private static final TypeReference[] CAFFEINE_TYPES = {
      TypeReference.of("com.github.benmanes.caffeine.cache.SSSW"),
      TypeReference.of("com.github.benmanes.caffeine.cache.SSSMW"),
      TypeReference.of("com.github.benmanes.caffeine.cache.PSW"),
      TypeReference.of("com.github.benmanes.caffeine.cache.PSMW"),
  };

  @Override
  public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
    for (var type : CAFFEINE_TYPES) {
      hints.reflection().registerType(type,
          INVOKE_PUBLIC_CONSTRUCTORS, INVOKE_DECLARED_CONSTRUCTORS);
    }
  }

}
