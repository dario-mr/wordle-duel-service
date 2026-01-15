package com.dariom.wds.config.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.dariom.wds.config.security.SecurityProperties.JwtProperties;
import com.dariom.wds.config.security.SecurityProperties.RefreshProperties;
import org.junit.jupiter.api.Test;

class SecurityPropertiesTest {

  @Test
  void whitelistPatternsArray_null_returnsEmptyArray() {
    // Arrange
    var props = new SecurityProperties(
        null,
        new JwtProperties("issuer", 900, "secret"),
        new RefreshProperties(7, "wd_refresh", "Lax", "/", false)
    );

    // Act
    var result = props.whitelistPatternsArray();

    // Assert
    assertThat(result).isEmpty();
  }

  @Test
  void whitelistPatternsArray_blank_returnsEmptyArray() {
    // Arrange
    var props = new SecurityProperties(
        "  ",
        new JwtProperties("issuer", 900, "secret"),
        new RefreshProperties(7, "wd_refresh", "Lax", "/", false)
    );

    // Act
    var result = props.whitelistPatternsArray();

    // Assert
    assertThat(result).isEmpty();
  }

  @Test
  void whitelistPatternsArray_hasBlanks_filtersAndTrims() {
    // Arrange
    var props = new SecurityProperties(
        " /actuator/** , , /swagger-ui/** ",
        new JwtProperties("issuer", 900, "secret"),
        new RefreshProperties(7, "wd_refresh", "Lax", "/", false)
    );

    // Act
    var result = props.whitelistPatternsArray();

    // Assert
    assertThat(result).containsExactly("/actuator/**", "/swagger-ui/**");
  }
}
