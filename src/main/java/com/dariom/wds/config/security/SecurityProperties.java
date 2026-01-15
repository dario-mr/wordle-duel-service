package com.dariom.wds.config.security;

import java.util.Arrays;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
    String whitelistAntPatterns,
    JwtProperties jwt,
    RefreshProperties refresh
) {

  public String[] whitelistPatternsArray() {
    if (whitelistAntPatterns == null || whitelistAntPatterns.isBlank()) {
      return new String[0];
    }

    return Arrays.stream(whitelistAntPatterns.split("\\s*,\\s*"))
        .map(String::trim)
        .filter(p -> !p.isBlank())
        .toArray(String[]::new);
  }

  public record JwtProperties(
      String issuer,
      int ttlSeconds,
      String secret
  ) {

  }

  public record RefreshProperties(
      int ttlDays,
      String cookieName,
      String cookieSameSite,
      String cookiePath,
      boolean cookieSecure
  ) {

  }
}
