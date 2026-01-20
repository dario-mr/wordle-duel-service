package com.dariom.wds.config.swagger;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SwaggerUiCsrfConfigTest {

  @Test
  void patchCsrfTokenNames_containsDefaults_replacesWithCustomNames() {
    // Arrange
    var js = "cookieName: \"XSRF-TOKEN\", headerName: \"X-XSRF-TOKEN\"";

    // Act
    var patched = SwaggerUiCsrfConfig.patchCsrfTokenNames(js, "WD-XSRF-TOKEN", "X-WD-XSRF-TOKEN");

    // Assert
    assertThat(patched)
        .contains("WD-XSRF-TOKEN")
        .contains("X-WD-XSRF-TOKEN")
        .doesNotContain("\"XSRF-TOKEN\"")
        .doesNotContain("\"X-XSRF-TOKEN\"");
  }

  @Test
  void patchCsrfTokenNames_noDefaults_returnsUnchanged() {
    // Arrange
    var js = "no csrf here";

    // Act
    var patched = SwaggerUiCsrfConfig.patchCsrfTokenNames(js, "WD-XSRF-TOKEN", "X-WD-XSRF-TOKEN");

    // Assert
    assertThat(patched).isEqualTo(js);
  }
}
