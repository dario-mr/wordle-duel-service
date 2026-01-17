package com.dariom.wds.service.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;
import org.junit.jupiter.api.Test;

class RefreshTokenGeneratorTest {

  private final RefreshTokenGenerator generator = new RefreshTokenGenerator();

  @Test
  void generate_returnsUrlSafeBase64WithoutPaddingWithExpectedLength() {
    // Arrange
    var urlDecoder = Base64.getUrlDecoder();

    // Act
    var token = generator.generate();

    // Assert
    assertThat(token).hasSize(64);
    assertThat(token).matches("[A-Za-z0-9_-]+$");
    assertThat(urlDecoder.decode(token)).hasSize(48);
  }

  @Test
  void generate_calledTwice_returnsDifferentValues() {
    // Act
    var token1 = generator.generate();
    var token2 = generator.generate();

    // Assert
    assertThat(token1).isNotEqualTo(token2);
  }
}
