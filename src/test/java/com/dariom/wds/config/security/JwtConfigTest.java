package com.dariom.wds.config.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class JwtConfigTest {

  @Test
  void rsaKeyPairFromPem_nullPrivateKey_throws() {
    // Act
    var thrown = catchThrowable(() -> JwtConfig.rsaKeyPairFromPem(null, "public"));

    // Assert
    assertThat(thrown)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Missing required property: app.security.jwt.private-key-pem");
  }

  @Test
  void rsaKeyPairFromPem_blankPublicKey_throws() {
    // Act
    var thrown = catchThrowable(() -> JwtConfig.rsaKeyPairFromPem("private", " "));

    // Assert
    assertThat(thrown)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Missing required property: app.security.jwt.public-key-pem");
  }

  @Test
  void rsaKeyPairFromPem_validPem_returnsRsaKeys() throws Exception {
    // Arrange
    var keyPairGenerator = KeyPairGenerator.getInstance("RSA");
    keyPairGenerator.initialize(2048);
    var keyPair = keyPairGenerator.generateKeyPair();
    var privateKeyPem = toPem("PRIVATE KEY", keyPair.getPrivate().getEncoded());
    var publicKeyPem = toPem("PUBLIC KEY", keyPair.getPublic().getEncoded());

    // Act
    var rsaKeyPair = JwtConfig.rsaKeyPairFromPem(privateKeyPem, publicKeyPem);

    // Assert
    assertThat(rsaKeyPair.privateKey()).isInstanceOf(RSAPrivateKey.class);
    assertThat(rsaKeyPair.publicKey()).isInstanceOf(RSAPublicKey.class);
  }

  @Test
  void rsaKeyPairFromPem_quotedEscapedPem_returnsRsaKeys() throws Exception {
    // Arrange
    var keyPairGenerator = KeyPairGenerator.getInstance("RSA");
    keyPairGenerator.initialize(2048);
    var keyPair = keyPairGenerator.generateKeyPair();
    var privateKeyPem = "'" + toPem("PRIVATE KEY", keyPair.getPrivate().getEncoded())
        .replace("\n", "\\n") + "'";
    var publicKeyPem = "'" + toPem("PUBLIC KEY", keyPair.getPublic().getEncoded())
        .replace("\n", "\\n") + "'";

    // Act
    var rsaKeyPair = JwtConfig.rsaKeyPairFromPem(privateKeyPem, publicKeyPem);

    // Assert
    assertThat(rsaKeyPair.privateKey()).isInstanceOf(RSAPrivateKey.class);
    assertThat(rsaKeyPair.publicKey()).isInstanceOf(RSAPublicKey.class);
  }

  @Test
  void audienceValidator_matchingAudience_returnsSuccess() {
    // Arrange
    var validator = JwtConfig.audienceValidator("wordle-duel");
    var jwt = Jwt.withTokenValue("token")
        .header("alg", "RS256")
        .subject("user-1")
        .audience(java.util.List.of("wordle-duel"))
        .claim("roles", java.util.List.of("USER"))
        .build();

    // Act
    var result = validator.validate(jwt);

    // Assert
    assertThat(result.hasErrors()).isFalse();
  }

  @Test
  void audienceValidator_missingAudience_returnsFailure() {
    // Arrange
    var validator = JwtConfig.audienceValidator("wordle-duel");
    var jwt = Jwt.withTokenValue("token")
        .header("alg", "RS256")
        .subject("user-1")
        .audience(java.util.List.of("other-app"))
        .claim("roles", java.util.List.of("USER"))
        .build();

    // Act
    var result = validator.validate(jwt);

    // Assert
    assertThat(result.hasErrors()).isTrue();
  }

  private static String toPem(String type, byte[] encoded) {
    var body = Base64.getMimeEncoder(64, "\n".getBytes())
        .encodeToString(encoded);
    return "-----BEGIN " + type + "-----\n"
        + body
        + "\n-----END " + type + "-----";
  }
}
