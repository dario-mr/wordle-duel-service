package com.dariom.wds.config.security;

import static com.dariom.wds.service.auth.OAuthUserService.APP_USER_ID_CLAIM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

class AuthenticatedUserResolverTest {

  private final AuthenticatedUserResolver resolver = new AuthenticatedUserResolver();

  @Test
  void from_validOidcUser_returnsAuthenticatedUser() {
    // Arrange
    var oidcUser = oidcUser(Map.of(
        APP_USER_ID_CLAIM, "user-1",
        "sub", "google-sub",
        "email", "user@example.com"
    ), Set.of("USER", "ADMIN"));

    // Act
    var user = resolver.from(oidcUser);

    // Assert
    assertThat(user.userId()).isEqualTo("user-1");
    assertThat(user.email()).isEqualTo("user@example.com");
    assertThat(user.roles()).containsExactlyInAnyOrder("USER", "ADMIN");
  }

  @Test
  void from_blankEmail_returnsEmptyEmail() {
    // Arrange
    var oidcUser = oidcUser(Map.of(
        APP_USER_ID_CLAIM, "user-1",
        "sub", "google-sub",
        "email", "   "
    ), Set.of());

    // Act
    var user = resolver.from(oidcUser);

    // Assert
    assertThat(user.email()).isEmpty();
  }

  @Test
  void from_missingAppUserId_throws() {
    // Arrange
    var oidcUser = oidcUser(Map.of("email", "user@example.com"), Set.of());

    // Act / Assert
    assertThatThrownBy(() -> resolver.from(oidcUser))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("OIDC principal is missing local app user id");
  }

  @Test
  void from_missingRoleAuthorities_returnsEmptySet() {
    // Arrange
    var oidcUser = oidcUser(Map.of(
        APP_USER_ID_CLAIM, "user-1",
        "sub", "google-sub",
        "email", "user@example.com"
    ), Set.of());

    // Act
    var user = resolver.from(oidcUser);

    // Assert
    assertThat(user.roles()).isEmpty();
  }

  private static OidcUser oidcUser(Map<String, Object> claims, Set<String> roles) {
    var now = Instant.parse("2025-01-01T00:00:00Z");
    var idToken = new OidcIdToken("id-token", now, now.plusSeconds(300), claims);
    var authorities = roles.stream()
        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
        .collect(Collectors.toSet());
    return new DefaultOidcUser(authorities, idToken, new OidcUserInfo(claims), "email");
  }
}
