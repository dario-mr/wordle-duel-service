package com.dariom.wds.config.security;

import static java.util.Base64.getDecoder;
import static org.springframework.security.oauth2.jwt.JwtValidators.createDefaultWithIssuer;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
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
    var keyPair = rsaKeyPair();
    var rsaKey = new RSAKey.Builder(keyPair.publicKey())
        .privateKey(keyPair.privateKey())
        .build();
    return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
  }

  @Bean
  JwtDecoder jwtDecoder() {
    var props = securityProperties.jwt();
    var keyPair = rsaKeyPair();

    var decoder = NimbusJwtDecoder
        .withPublicKey(keyPair.publicKey())
        .build();

    decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
        createDefaultWithIssuer(props.issuer()),
        audienceValidator(props.audience())
    ));
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

  static OAuth2TokenValidator<Jwt> audienceValidator(String expectedAudience) {
    return jwt -> {
      var audience = jwt.getAudience();
      if (audience != null && audience.contains(expectedAudience)) {
        return OAuth2TokenValidatorResult.success();
      }

      var error = new OAuth2Error("invalid_token", "The required audience is missing", null);
      return OAuth2TokenValidatorResult.failure(error);
    };
  }

  static RsaKeyPair rsaKeyPairFromPem(String privateKeyPem, String publicKeyPem) {
    if (privateKeyPem == null || privateKeyPem.isBlank()) {
      throw new IllegalStateException(
          "Missing required property: app.security.jwt.private-key-pem");
    }
    if (publicKeyPem == null || publicKeyPem.isBlank()) {
      throw new IllegalStateException("Missing required property: app.security.jwt.public-key-pem");
    }

    try {
      var keyFactory = KeyFactory.getInstance("RSA");
      var privateKey = (RSAPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(
          decodePem(privateKeyPem, "PRIVATE KEY")));
      var publicKey = (RSAPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(
          decodePem(publicKeyPem, "PUBLIC KEY")));
      return new RsaKeyPair(privateKey, publicKey);
    } catch (GeneralSecurityException ex) {
      throw new IllegalStateException("Failed to parse RSA key material", ex);
    }
  }

  private RsaKeyPair rsaKeyPair() {
    var jwtProperties = securityProperties.jwt();
    return rsaKeyPairFromPem(jwtProperties.privateKeyPem(), jwtProperties.publicKeyPem());
  }

  private static byte[] decodePem(String pem, String type) {
    var trimmedPem = pem.trim();
    var unquotedPem = trimmedPem.length() >= 2
        && ((trimmedPem.startsWith("\"") && trimmedPem.endsWith("\""))
        || (trimmedPem.startsWith("'") && trimmedPem.endsWith("'")))
        ? trimmedPem.substring(1, trimmedPem.length() - 1)
        : trimmedPem;
    var normalizedPem = unquotedPem
        .replace("\\r", "\r")
        .replace("\\n", "\n");
    var normalized = normalizedPem
        .replace("-----BEGIN " + type + "-----", "")
        .replace("-----END " + type + "-----", "")
        .replaceAll("\\s", "");
    return getDecoder().decode(normalized);
  }

  record RsaKeyPair(RSAPrivateKey privateKey, RSAPublicKey publicKey) {

  }
}
