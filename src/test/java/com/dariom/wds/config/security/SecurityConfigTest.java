package com.dariom.wds.config.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;

import com.dariom.wds.service.auth.OAuthUserService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.util.matcher.RequestMatcher;

class SecurityConfigTest {

  @Test
  void csrfTokenRepository_csrfPropertiesMissing_throwsIllegalStateException() {
    // Arrange
    var props = new SecurityProperties(null, null, null, null, null);
    var config = new SecurityConfig(props);

    // Act
    var thrown = catchThrowable(config::csrfTokenRepository);

    // Assert
    assertThat(thrown)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("app.security.csrf");
  }

  @Test
  void apiSecurityFilterChain_matcherPropertiesMissing_throwsIllegalStateException() {
    // Arrange
    var props = new SecurityProperties(null,
        new SecurityProperties.CsrfProperties("cookie", "header"), null, null, null);
    var config = new SecurityConfig(props);

    // Act
    var thrown = catchThrowable(config::apiAndAdminRequestMatcher);

    // Assert
    assertThat(thrown)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("app.security.matcher");
  }

  @Test
  void apiAndAdminRequestMatcher_matchesApiAndAdminRoutesOnly() {
    // Arrange
    var props = new SecurityProperties(
        null,
        new SecurityProperties.CsrfProperties("cookie", "header"),
        new SecurityProperties.MatcherProperties("/api/**", "/admin/**", "/auth/**"),
        null,
        null
    );
    var config = new SecurityConfig(props);

    // Act
    var matcher = config.apiAndAdminRequestMatcher();

    // Assert
    assertThat(matches(matcher, "POST", "/api/v1/rooms")).isTrue();
    assertThat(matches(matcher, "DELETE", "/admin/rooms/1")).isTrue();
    assertThat(matches(matcher, "POST", "/auth/refresh")).isFalse();
  }

  @Test
  void oidcUserService_returnsConcreteDelegatingService() {
    var props = new SecurityProperties(
        // Arrange
        null,
        new SecurityProperties.CsrfProperties("cookie", "header"),
        new SecurityProperties.MatcherProperties("/api/**", "/admin/**", "/auth/**"),
        null,
        null
    );
    var config = new SecurityConfig(props);

    // Act
    var service = config.oidcUserService(mock(OAuthUserService.class));

    // Assert
    assertThat(service).isInstanceOf(DelegatingOidcUserService.class);
  }

  private static boolean matches(RequestMatcher matcher, String method, String path) {
    var request = new MockHttpServletRequest(method, path);
    request.setServletPath(path);

    return matcher.matches(request);
  }
}
