package com.dariom.wds.config.nativeimage;

import static org.springframework.aot.hint.MemberCategory.DECLARED_FIELDS;
import static org.springframework.aot.hint.MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS;
import static org.springframework.aot.hint.MemberCategory.INVOKE_PUBLIC_METHODS;

import com.dariom.wds.domain.RoundPlayerStatus;
import com.dariom.wds.websocket.model.PlayerJoinedPayload;
import com.dariom.wds.websocket.model.RematchStartedPayload;
import com.dariom.wds.websocket.model.RoomEvent;
import com.dariom.wds.websocket.model.RoomEventToPublish;
import com.dariom.wds.websocket.model.ScoresUpdatedPayload;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

public class WebSocketRuntimeHints implements RuntimeHintsRegistrar {

  @Override
  public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
    registerRecord(hints, RoomEventToPublish.class);
    registerRecord(hints, RoomEvent.class);
    registerRecord(hints, PlayerJoinedPayload.class);
    registerRecord(hints, ScoresUpdatedPayload.class);
    registerRecord(hints, RematchStartedPayload.class);
    hints.reflection().registerType(RoundPlayerStatus.class,
        DECLARED_FIELDS, INVOKE_PUBLIC_METHODS);
  }

  private static void registerRecord(RuntimeHints hints, Class<?> type) {
    hints.reflection().registerType(type,
        INVOKE_PUBLIC_CONSTRUCTORS,
        INVOKE_PUBLIC_METHODS,
        DECLARED_FIELDS);
  }

}
