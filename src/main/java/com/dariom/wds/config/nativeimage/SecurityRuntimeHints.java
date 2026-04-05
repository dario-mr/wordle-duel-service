package com.dariom.wds.config.nativeimage;

import java.io.Serializable;
import java.util.List;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponseType;

public class SecurityRuntimeHints implements RuntimeHintsRegistrar {

  private static final List<Class<? extends Serializable>> SERIALIZABLE_TYPES = List.of(
      OAuth2AuthorizationRequest.class,
      OAuth2AuthorizationResponseType.class,
      AuthorizationGrantType.class,
      ClientAuthenticationMethod.class
  );

  private static final List<TypeReference> SERIALIZABLE_JDK_TYPES = List.of(
      TypeReference.of("java.util.Collections$UnmodifiableMap"),
      TypeReference.of("java.util.Collections$UnmodifiableSet"),
      TypeReference.of("java.util.Collections$UnmodifiableCollection"),
      TypeReference.of("java.util.Collections$UnmodifiableList"),
      TypeReference.of("java.util.Collections$UnmodifiableRandomAccessList")
  );

  @Override
  public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
    for (var type : SERIALIZABLE_TYPES) {
      hints.serialization().registerType(type);
    }
    for (var type : SERIALIZABLE_JDK_TYPES) {
      hints.serialization().registerType(type);
    }
  }

}
