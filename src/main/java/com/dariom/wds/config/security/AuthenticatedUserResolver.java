package com.dariom.wds.config.security;

import static com.dariom.wds.service.auth.OAuthUserService.APP_USER_ID_CLAIM;
import static java.util.stream.Collectors.toCollection;
import static org.springframework.util.StringUtils.hasText;

import java.util.LinkedHashSet;
import java.util.Optional;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedUserResolver {

  public AuthenticatedUser from(OidcUser oidcUser) {
    var userId = Optional.ofNullable(oidcUser.<String>getAttribute(APP_USER_ID_CLAIM))
        .filter(value -> hasText(value))
        .orElseThrow(() -> new IllegalStateException("OIDC principal is missing local app user id"));
    var email = Optional.ofNullable(oidcUser.<String>getAttribute("email"))
        .filter(value -> hasText(value))
        .orElse("");
    var roles = oidcUser.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .filter(authority -> authority.startsWith("ROLE_"))
        .map(authority -> authority.substring("ROLE_".length()))
        .collect(toCollection(LinkedHashSet::new));

    return new AuthenticatedUser(userId, email, roles);
  }
}
