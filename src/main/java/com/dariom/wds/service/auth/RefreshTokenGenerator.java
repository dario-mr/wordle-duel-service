package com.dariom.wds.service.auth;

import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenGenerator {

  private final SecureRandom secureRandom = new SecureRandom();

  public String generate() {
    var bytes = new byte[48];
    secureRandom.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }
}
