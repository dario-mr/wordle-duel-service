package com.dariom.wds.config.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.Test;

class JwtConfigTest {

  @Test
  void hmacKeyFromSecret_null_throws() {
    // Act
    var thrown = catchThrowable(() -> JwtConfig.hmacKeyFromSecret(null));

    // Assert
    assertThat(thrown)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Missing required property: app.security.jwt.secret");
  }

  @Test
  void hmacKeyFromSecret_blank_throws() {
    // Arrange
    var secret = " ";

    // Act
    var thrown = catchThrowable(() -> JwtConfig.hmacKeyFromSecret(secret));

    // Assert
    assertThat(thrown)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Missing required property: app.security.jwt.secret");
  }

  @Test
  void hmacKeyFromSecret_tooShort_throws() {
    // Arrange
    var secret = "a".repeat(31);

    // Act
    var thrown = catchThrowable(() -> JwtConfig.hmacKeyFromSecret(secret));

    // Assert
    assertThat(thrown)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("app.security.jwt.secret must be at least 32 bytes for HS256");
  }

  @Test
  void hmacKeyFromSecret_longEnough_returnsHmacSha256Key() {
    // Arrange
    var secret = "a".repeat(32);

    // Act
    var key = JwtConfig.hmacKeyFromSecret(secret);

    // Assert
    assertThat(key.getAlgorithm()).isEqualTo("HmacSHA256");
    assertThat(key.getEncoded()).hasSize(32);
  }
}
