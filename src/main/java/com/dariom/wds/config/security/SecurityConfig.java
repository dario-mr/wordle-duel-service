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
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

/**
 * Configures Spring Security using two distinct {@link SecurityFilterChain}s.
 *
 * <p><b>1) API filter chain</b>
 *
 * <ul>
 *   <li>Applies to {@code /api/**} and {@code /admin/**}.</li>
 *   <li>Stateless: no HTTP session is created/used.</li>
 *   <li>CSRF is disabled because these endpoints are called with {@code Authorization: Bearer <JWT>}.</li>
 *   <li>Authentication is performed by the OAuth2 Resource Server (JWT) support.</li>
 * </ul>
 *
 * <p><b>2) Auth/OAuth filter chain</b>
 *
 * <ul>
 *   <li>Handles OAuth2 login endpoints and auth endpoints ({@code POST /auth/refresh}, {@code POST /auth/logout}),
 *       plus other explicit allowlisted endpoints (e.g. health, Swagger, H2 console depending on configuration).</li>
 *   <li>State is allowed only when required for the OAuth2 login flow ({@code SessionCreationPolicy.IF_REQUIRED}).</li>
 *   <li>CSRF is enabled using a cookie-based token repository (SPA-friendly): the browser stores {@code XSRF-TOKEN}
 *       and clients must send it back as {@code X-XSRF-TOKEN} for state-changing requests.</li>
 * </ul>
 *
 * <p>The split keeps the API strictly stateless (Bearer JWT) while still supporting browser-based OAuth2 login and
 * refresh-token rotation via HttpOnly cookies.</p>
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private static final String API_MATCHER = "/api/**";
  private static final String ADMIN_MATCHER = "/admin/**";
  private static final String AUTH_MATCHER = "/auth/**";

  private static final String CSRF_COOKIE_NAME = "WD-XSRF-TOKEN";
  private static final String CSRF_HEADER_NAME = "X-WD-XSRF-TOKEN";

  private final SecurityProperties securityProperties;

  @Bean
  @Order(1)
  SecurityFilterChain apiSecurityFilterChain(
      HttpSecurity http,
      JwtAuthenticationConverter jwtAuthenticationConverter
  ) throws Exception {
    http
        .securityMatcher(API_MATCHER, ADMIN_MATCHER)
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(sm -> sm.sessionCreationPolicy(STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(ADMIN_MATCHER).hasRole(ADMIN.getName())
            .requestMatchers(API_MATCHER).authenticated()
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
      OAuthUserService oAuthUserService
  ) throws Exception {
    http
        .csrf(csrf -> csrf
            .csrfTokenRepository(csrfTokenRepository)
            .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
            .ignoringRequestMatchers("/h2-console/**")
        )
        .sessionManagement(sm -> sm.sessionCreationPolicy(IF_REQUIRED))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(securityProperties.whitelistPatternsArray()).permitAll()
            .requestMatchers(AUTH_MATCHER).permitAll()
            .anyRequest().denyAll()
        )
        .oauth2Login(oauth -> oauth
            .userInfoEndpoint(user -> user.oidcUserService(oidcUserService(oAuthUserService)))
            .successHandler(oauth2SuccessHandler)
        )
        .logout(AbstractHttpConfigurer::disable)
        .headers(headers -> headers
            .frameOptions(FrameOptionsConfig::sameOrigin)
        );

    return http.build();
  }

  @Bean
  CookieCsrfTokenRepository csrfTokenRepository() {
    var repository = withHttpOnlyFalse();
    repository.setCookieName(CSRF_COOKIE_NAME);
    repository.setHeaderName(CSRF_HEADER_NAME);
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
  OidcUserService oidcUserService(OAuthUserService oAuthUserService) {
    var delegate = new OidcUserService();
    return new OidcUserService() {
      @Override
      public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        var oidcUser = delegate.loadUser(userRequest);
        return oAuthUserService.createOrUpdatePrincipal(oidcUser);
      }
    };
  }

}
