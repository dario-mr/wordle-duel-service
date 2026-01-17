package com.dariom.wds.config.security;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256;
import static org.springframework.security.oauth2.jwt.JwtValidators.createDefaultWithIssuer;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

@Configuration
@RequiredArgsConstructor
public class JwtConfig {

  private final SecurityProperties securityProperties;

  @Bean
  JwtEncoder jwtEncoder() {
    return new NimbusJwtEncoder(new ImmutableSecret<>(hmacKey()));
  }

  @Bean
  JwtDecoder jwtDecoder() {
    var props = securityProperties.jwt();

    var decoder = NimbusJwtDecoder
        .withSecretKey(hmacKey())
        .macAlgorithm(HS256)
        .build();

    decoder.setJwtValidator(createDefaultWithIssuer(props.issuer()));
    return decoder;
  }

  @Bean
  JwtAuthenticationConverter jwtAuthenticationConverter() {
    var authoritiesConverter = new JwtGrantedAuthoritiesConverter();
    authoritiesConverter.setAuthorityPrefix("ROLE_");
    authoritiesConverter.setAuthoritiesClaimName("roles");

    var converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
    return converter;
  }

  static SecretKey hmacKeyFromSecret(String secret) {
    if (secret == null || secret.isBlank()) {
      throw new IllegalStateException("Missing required property: app.security.jwt.secret");
    }

    var secretBytes = secret.getBytes(UTF_8);
    if (secretBytes.length < 32) {
      throw new IllegalStateException(
          "app.security.jwt.secret must be at least 32 bytes for HS256");
    }

    return new SecretKeySpec(secretBytes, "HmacSHA256");
  }

  private SecretKey hmacKey() {
    return hmacKeyFromSecret(securityProperties.jwt().secret());
  }

}
