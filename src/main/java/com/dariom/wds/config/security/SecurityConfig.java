package com.dariom.wds.config.security;

import static com.dariom.wds.domain.Role.ADMIN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.security.config.http.SessionCreationPolicy.IF_REQUIRED;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;
import static org.springframework.security.web.csrf.CookieCsrfTokenRepository.withHttpOnlyFalse;

import com.dariom.wds.persistence.repository.UserRepository;
import com.dariom.wds.service.auth.OAuth2RefreshCookieSuccessHandler;
import com.dariom.wds.service.auth.OAuthUserService;
import com.dariom.wds.service.auth.RefreshTokenCookieService;
import com.dariom.wds.service.auth.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Configures Spring Security using two distinct {@link SecurityFilterChain}s.
 *
 * <p><b>1) API filter chain</b>
 *
 * <ul>
 *   <li>Applies to {@code /api/**} and {@code /admin/**}.</li>
 *   <li>Stateless: no HTTP session is created/used.</li>
 *   <li>CSRF is ignored for these endpoints because they are called with
 *       {@code Authorization: Bearer <JWT>} rather than browser-managed cookies.</li>
 *   <li>Authentication is performed by the OAuth2 Resource Server (JWT) support.</li>
 * </ul>
 *
 * <p><b>2) Auth/OAuth filter chain</b>
 *
 * <ul>
 *   <li>Handles OAuth2 login endpoints and auth endpoints ({@code POST /auth/refresh}, {@code POST /auth/logout}),
 *       plus other explicit allowlisted endpoints (e.g. health, Swagger, H2 console depending on configuration).</li>
 *   <li>State is allowed only when required for the OAuth2 login flow ({@code SessionCreationPolicy.IF_REQUIRED}).</li>
 *   <li>CSRF is enabled using a cookie-based token repository: the browser stores a CSRF cookie and clients must
 *       send it back as a header for state-changing requests (names are configured via {@code app.security.csrf.*}).</li>
 * </ul>
 *
 * <p>The split keeps the API strictly stateless (Bearer JWT) while still supporting browser-based OAuth2 login and
 * refresh-token rotation via HttpOnly cookies.</p>
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final SecurityProperties securityProperties;

  @Bean
  @Order(1)
  SecurityFilterChain apiSecurityFilterChain(
      HttpSecurity http,
      JwtAuthenticationConverter jwtAuthenticationConverter
  ) throws Exception {
    var matcher = requireMatcherProperties();
    var apiMatcher = matcher.api();
    var adminMatcher = matcher.admin();
    var apiAndAdminMatcher = apiAndAdminRequestMatcher();

    http
        .securityMatcher(apiAndAdminMatcher)
        .csrf(csrf -> csrf.ignoringRequestMatchers(apiAndAdminMatcher))
        .sessionManagement(sm -> sm
            .sessionCreationPolicy(STATELESS)
            // Prevent Spring Security from attempting session fixation protection (changeSessionId)
            // when a browser sends a SESSION cookie alongside Bearer JWT requests.
            .sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy())
        )
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(adminMatcher).hasRole(ADMIN.getName())
            .requestMatchers(apiMatcher).authenticated()
            .anyRequest().denyAll()
        )
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint(new HttpStatusEntryPoint(UNAUTHORIZED))
        )
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
        )
        .logout(AbstractHttpConfigurer::disable);

    return http.build();
  }

  @Bean
  @Order(2)
  SecurityFilterChain authSecurityFilterChain(
      HttpSecurity http,
      CookieCsrfTokenRepository csrfTokenRepository,
      AuthenticationSuccessHandler oauth2SuccessHandler,
      DelegatingOidcUserService oidcUserService
  ) throws Exception {
    var matcher = requireMatcherProperties();

    http
        .csrf(csrf -> csrf
            .csrfTokenRepository(csrfTokenRepository)
            .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
            .ignoringRequestMatchers("/h2-console/**")
        )
        .sessionManagement(sm -> sm.sessionCreationPolicy(IF_REQUIRED))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(securityProperties.whitelistPatternsArray()).permitAll()
            .requestMatchers(matcher.auth()).permitAll()
            .anyRequest().denyAll()
        )
        .oauth2Login(oauth -> oauth
            .userInfoEndpoint(user -> user.oidcUserService(oidcUserService))
            .successHandler(oauth2SuccessHandler)
        )
        .logout(AbstractHttpConfigurer::disable)
        .headers(headers -> headers
            .frameOptions(FrameOptionsConfig::sameOrigin)
        )
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint(new HttpStatusEntryPoint(UNAUTHORIZED))
        );

    return http.build();
  }

  @Bean
  CookieCsrfTokenRepository csrfTokenRepository() {
    var csrfProperties = requireCsrfProperties();

    var repository = withHttpOnlyFalse();
    repository.setCookieName(csrfProperties.cookieName());
    repository.setHeaderName(csrfProperties.headerName());
    repository.setCookiePath("/");

    return repository;
  }

  @Bean
  AuthenticationSuccessHandler oauth2SuccessHandler(
      @Value("${app.frontend.success-redirect}") String target,
      UserRepository userRepository,
      RefreshTokenService refreshTokenService,
      RefreshTokenCookieService refreshTokenCookieService
  ) {
    var successHandler = new SimpleUrlAuthenticationSuccessHandler(target);
    successHandler.setAlwaysUseDefaultTargetUrl(true);

    return new OAuth2RefreshCookieSuccessHandler(
        userRepository,
        refreshTokenService,
        refreshTokenCookieService,
        successHandler
    );
  }

  @Bean
  DelegatingOidcUserService oidcUserService(OAuthUserService oAuthUserService) {
    return new DelegatingOidcUserService(oAuthUserService);
  }

  RequestMatcher apiAndAdminRequestMatcher() {
    var matcher = requireMatcherProperties();

    return new OrRequestMatcher(
        PathPatternRequestMatcher.withDefaults().matcher(matcher.api()),
        PathPatternRequestMatcher.withDefaults().matcher(matcher.admin())
    );
  }

  private SecurityProperties.CsrfProperties requireCsrfProperties() {
    var csrf = securityProperties.csrf();
    if (csrf == null) {
      throw new IllegalStateException("Missing required property: app.security.csrf");
    }

    return csrf;
  }

  private SecurityProperties.MatcherProperties requireMatcherProperties() {
    var matcher = securityProperties.matcher();
    if (matcher == null) {
      throw new IllegalStateException("Missing required property: app.security.matcher");
    }

    return matcher;
  }

}
