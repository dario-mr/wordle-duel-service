package com.dariom.wds.common;

import com.dariom.wds.config.security.SecurityProperties;

public class SecurityPropertiesBuilder {

  private SecurityPropertiesBuilder() {

  }

  public static SecurityProperties build() {
    return new SecurityProperties(
        "",
        null,
        null,
        null,
        null
    );
  }

}
