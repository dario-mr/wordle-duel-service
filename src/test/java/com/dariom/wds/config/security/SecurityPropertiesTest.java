package com.dariom.wds.config.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SecurityPropertiesTest {

  @Test
  void whitelistPatternsArray_null_returnsEmptyArray() {
    // Arrange
    var props = new SecurityProperties(null, null, null, null, null);

    // Act
    var result = props.whitelistPatternsArray();

    // Assert
    assertThat(result).isEmpty();
  }

  @Test
  void whitelistPatternsArray_blank_returnsEmptyArray() {
    // Arrange
    var props = new SecurityProperties("  ", null, null, null, null);

    // Act
    var result = props.whitelistPatternsArray();

    // Assert
    assertThat(result).isEmpty();
  }

  @Test
  void whitelistPatternsArray_hasBlanks_filtersAndTrims() {
    // Arrange
    var props = new SecurityProperties(" /actuator/** , , /swagger-ui/** ", null, null, null, null);

    // Act
    var result = props.whitelistPatternsArray();

    // Assert
    assertThat(result).containsExactly("/actuator/**", "/swagger-ui/**");
  }
}
