package com.dariom.wds.service.auth;

import static java.util.stream.Collectors.toSet;

import com.dariom.wds.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

/**
 * Service responsible for mapping an externally authenticated OAuth2/OIDC user (Google) to a local
 * application user and assigning application-specific roles.
 *
 * <p><b>When is this class used?</b></p>
 * <ul>
 *   <li>Exactly once per successful OAuth2/OIDC login</li>
 *   <li>During the {@code /login/oauth2/code/google} callback processing</li>
 * </ul>
 *
 * <p><b>What does it do?</b></p>
 * <ul>
 *   <li>Extracts identity claims from the OAuth2/OIDC principal
 *       (email, Google {@code sub})</li>
 *   <li>Creates or updates a local {@code AppUserEntity} record</li>
 *   <li>Ensures default roles (e.g. {@code USER}) are assigned</li>
 *   <li>Returns a Spring Security principal containing application roles</li>
 * </ul>
 *
 * <p><b>Why does this exist?</b></p>
 * <p>
 * OAuth2 providers authenticate <em>identity</em>, but they do not know
 * application-specific concepts like {@code ADMIN} or {@code USER}.
 * This service bridges that gap.
 * </p>
 *
 * <p><b>What it does NOT do</b></p>
 * <ul>
 *   <li>Does NOT handle remember-me token restoration</li>
 *   <li>Does NOT run on every request</li>
 *   <li>Does NOT validate OAuth2 tokens (Spring does that)</li>
 * </ul>
 *
 * <p>
 * The returned principal uses the user's email as
 * {@link Authentication#getName()},
 * so that remember-me tokens can later reload the user consistently.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class OAuthUserService {

  private final UserRepository userRepository;

  public OidcUser createOrUpdatePrincipal(OidcUser oidcUser) {
    String email = oidcUser.getAttribute("email");
    var sub = oidcUser.getSubject();
    var fullName = oidcUser.getFullName();

    if (email == null || sub == null) {
      throw new IllegalStateException("Google user missing required claims (email/sub)");
    }

    var user = userRepository.findOrCreate(sub, email, fullName);

    var authorities = user.getRoles().stream()
        .map(r -> new SimpleGrantedAuthority("ROLE_" + r.getName()))
        .collect(toSet());

    return new DefaultOidcUser(authorities, oidcUser.getIdToken(), oidcUser.getUserInfo(), "email");
  }
}
