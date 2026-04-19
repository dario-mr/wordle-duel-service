package com.dariom.wds.config.security;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedUserResolver {

  public AuthenticatedUser from(Jwt jwt) {
    var userId = Optional.ofNullable(jwt.getSubject())
        .filter(subject -> !subject.isBlank())
        .orElseThrow(() -> new IllegalStateException("JWT is missing subject claim"));
    var email = Optional.ofNullable(jwt.getClaimAsString("email"))
        .filter(value -> !value.isBlank())
        .orElse("");
    var roles = new LinkedHashSet<>(Optional.ofNullable(jwt.getClaimAsStringList("roles"))
        .orElse(List.of()));

    return new AuthenticatedUser(userId, email, roles);
  }
}
