package com.dariom.wds.service.auth;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.stereotype.Component;

@Component
public class TokenHashing {

  public String sha256Hex(String value) {
    try {
      var digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(value.getBytes(UTF_8));
      return toHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }

  private static String toHex(byte[] bytes) {
    char[] out = new char[bytes.length * 2];
    int i = 0;
    for (byte b : bytes) {
      int v = b & 0xFF;
      out[i++] = Character.forDigit(v >>> 4, 16);
      out[i++] = Character.forDigit(v & 0x0F, 16);
    }
    return new String(out);
  }
}
