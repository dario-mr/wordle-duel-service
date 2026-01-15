package com.dariom.wds.service.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TokenHashingTest {

  private final TokenHashing tokenHashing = new TokenHashing();

  @Test
  void sha256Hex_sameInputTwice_returnsSameHash() {
    // Arrange
    var input = "hello";

    // Act
    var hash1 = tokenHashing.sha256Hex(input);
    var hash2 = tokenHashing.sha256Hex(input);

    // Assert
    assertThat(hash1)
        .isEqualTo("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824");
    assertThat(hash1).isEqualTo(hash2);
  }

  @Test
  void sha256Hex_differentInputs_returnsDifferentHashes() {
    // Arrange

    // Act
    var hash1 = tokenHashing.sha256Hex("token-1");
    var hash2 = tokenHashing.sha256Hex("token-2");

    // Assert
    assertThat(hash1).isNotEqualTo(hash2);
    assertThat(hash1).hasSize(64);
    assertThat(hash1).matches("[0-9a-f]+$");
  }
}
