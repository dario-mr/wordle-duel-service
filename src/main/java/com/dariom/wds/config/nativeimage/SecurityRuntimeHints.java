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
      TypeReference.of("java.lang.Boolean"),
      TypeReference.of("java.lang.Integer"),
      TypeReference.of("java.lang.Long"),
      TypeReference.of("java.lang.Number"),
      TypeReference.of("java.lang.String"),
      TypeReference.of("java.net.URL"),
      TypeReference.of("java.time.Instant"),
      TypeReference.of("java.time.Ser"),
      TypeReference.of("java.util.ArrayList"),
      TypeReference.of("java.util.Collections$UnmodifiableMap"),
      TypeReference.of("java.util.Collections$UnmodifiableSet"),
      TypeReference.of("java.util.Collections$UnmodifiableCollection"),
      TypeReference.of("java.util.Collections$UnmodifiableList"),
      TypeReference.of("java.util.Collections$UnmodifiableRandomAccessList"),
      TypeReference.of("java.util.HashMap"),
      TypeReference.of("java.util.HashSet"),
      TypeReference.of("java.util.LinkedHashMap"),
      TypeReference.of("java.util.LinkedHashSet"),
      TypeReference.of("org.springframework.security.authentication.AbstractAuthenticationToken"),
      TypeReference.of("org.springframework.security.core.authority.SimpleGrantedAuthority"),
      TypeReference.of("org.springframework.security.core.context.SecurityContextImpl"),
      TypeReference.of(
          "org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken"),
      TypeReference.of("org.springframework.security.oauth2.core.AbstractOAuth2Token"),
      TypeReference.of("org.springframework.security.oauth2.core.oidc.OidcIdToken"),
      TypeReference.of("org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser"),
      TypeReference.of("org.springframework.security.oauth2.core.user.DefaultOAuth2User"),
      TypeReference.of("org.springframework.security.web.authentication.WebAuthenticationDetails")
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
