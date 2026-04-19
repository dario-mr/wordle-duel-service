package com.dariom.wds.config.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class AuthenticatedUserResolverTest {

  private final AuthenticatedUserResolver resolver = new AuthenticatedUserResolver();

  @Test
  void from_validJwt_returnsAuthenticatedUser() {
    var jwt = jwt(Map.of(
        "sub", "user-1",
        "email", "user@example.com",
        "roles", List.of("USER", "ADMIN")
    ));

    var user = resolver.from(jwt);

    assertThat(user.userId()).isEqualTo("user-1");
    assertThat(user.email()).isEqualTo("user@example.com");
    assertThat(user.roles()).isEqualTo(Set.of("USER", "ADMIN"));
  }

  @Test
  void from_blankEmail_returnsEmptyEmail() {
    var jwt = jwt(Map.of(
        "sub", "user-1",
        "email", "   "
    ));

    var user = resolver.from(jwt);

    assertThat(user.email()).isEmpty();
  }

  @Test
  void from_missingSubject_throws() {
    assertThatThrownBy(() -> resolver.from(jwt(Map.of("email", "user@example.com"))))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("JWT is missing subject claim");
  }

  @Test
  void from_missingRoles_returnsEmptySet() {
    var user = resolver.from(jwt(Map.of(
        "sub", "user-1",
        "email", "user@example.com"
    )));

    assertThat(user.roles()).isEmpty();
  }

  private static Jwt jwt(Map<String, Object> claims) {
    var now = Instant.now();
    return new Jwt("token", now, now.plusSeconds(300), Map.of("alg", "RS256"), claims);
  }
}
