package com.dariom.wds.config.security;

import com.dariom.wds.service.auth.OAuthUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@RequiredArgsConstructor
class DelegatingOidcUserService extends OidcUserService {

  private final OAuthUserService oAuthUserService;
  private final OidcUserService delegate;

  DelegatingOidcUserService(OAuthUserService oAuthUserService) {
    this(oAuthUserService, new OidcUserService());
  }

  @Override
  public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
    var oidcUser = delegate.loadUser(userRequest);
    return oAuthUserService.createOrUpdatePrincipal(oidcUser);
  }

}
