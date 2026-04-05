package com.dariom.wds.config.nativeimage;

import static org.springframework.aot.hint.MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS;
import static org.springframework.aot.hint.MemberCategory.INVOKE_PUBLIC_METHODS;

import com.dariom.wds.persistence.entity.RoomPlayerIdEmbeddable;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

public class PersistenceRuntimeHints implements RuntimeHintsRegistrar {

  @Override
  public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
    hints.reflection().registerType(RoomPlayerIdEmbeddable.class,
        INVOKE_PUBLIC_CONSTRUCTORS,
        INVOKE_PUBLIC_METHODS);
  }

}
