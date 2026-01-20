package com.dariom.wds.config.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.Test;

class SecurityConfigTest {

  @Test
  void csrfTokenRepository_csrfPropertiesMissing_throwsIllegalStateException() {
    // Arrange
    var props = new SecurityProperties(null, null, null, null, null);
    var config = new SecurityConfig(props);

    // Act
    var thrown = catchThrowable(config::csrfTokenRepository);

    // Assert
    assertThat(thrown)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("app.security.csrf");
  }

  @Test
  void apiSecurityFilterChain_matcherPropertiesMissing_throwsIllegalStateException() {
    // Arrange
    var props = new SecurityProperties(null,
        new SecurityProperties.CsrfProperties("cookie", "header"), null, null, null);
    var config = new SecurityConfig(props);

    // Act
    var thrown = catchThrowable(() -> config.apiSecurityFilterChain(null, null));

    // Assert
    assertThat(thrown)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("app.security.matcher");
  }
}
