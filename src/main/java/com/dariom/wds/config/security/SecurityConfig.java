package com.dariom.wds.config.security;

import static com.dariom.wds.api.common.ErrorCode.UNAUTHENTICATED;
import static com.dariom.wds.domain.Role.ADMIN;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.security.config.http.SessionCreationPolicy.IF_REQUIRED;
import static org.springframework.security.web.csrf.CookieCsrfTokenRepository.withHttpOnlyFalse;

import com.dariom.wds.api.common.ErrorCode;
import com.dariom.wds.service.auth.OAuthUserService;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
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
 *   <li>Uses the Redis-backed Spring Security session.</li>
 *   <li>CSRF protects state-changing requests because authentication uses a browser cookie.</li>
 * </ul>
 *
 * <p><b>2) Auth/OAuth filter chain</b>
 *
 * <ul>
 *   <li>Handles OAuth2 login endpoints and the logout endpoint ({@code POST /auth/logout}),
 *       plus other explicit allowlisted endpoints (e.g. health, Swagger, H2 console depending on configuration).</li>
 *   <li>State is allowed only when required for the OAuth2 login flow ({@code SessionCreationPolicy.IF_REQUIRED}).</li>
 *   <li>CSRF is enabled using a cookie-based token repository: the browser stores a CSRF cookie and clients must
 *       send it back as a header for state-changing requests (names are configured via {@code app.security.csrf.*}).</li>
 * </ul>
 *
 * <p>Both chains use the same server-side session, so REST and WebSocket handshakes share authentication.</p>
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
      CookieCsrfTokenRepository csrfTokenRepository
  ) throws Exception {
    var matcher = requireMatcherProperties();
    var apiMatcher = matcher.api();
    var adminMatcher = matcher.admin();
    var apiAndAdminMatcher = apiAndAdminRequestMatcher();

    http
        .securityMatcher(apiAndAdminMatcher)
        .csrf(csrf -> csrf
            .csrfTokenRepository(csrfTokenRepository)
            .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
        .sessionManagement(sm -> sm.sessionCreationPolicy(IF_REQUIRED))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(adminMatcher).hasRole(ADMIN.getName())
            .requestMatchers(apiMatcher).authenticated()
            .anyRequest().denyAll()
        )
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint((request, response, exception) ->
                writeErrorResponse(response, UNAUTHORIZED.value(), UNAUTHENTICATED))
            .accessDeniedHandler((request, response, exception) ->
                writeErrorResponse(response, HttpStatus.FORBIDDEN.value(), ErrorCode.FORBIDDEN))
        );

    return http.build();
  }

  @Bean
  @Order(2)
  SecurityFilterChain authSecurityFilterChain(
      HttpSecurity http,
      CookieCsrfTokenRepository csrfTokenRepository,
      AuthenticationSuccessHandler oauth2SuccessHandler,
      DelegatingOidcUserService oidcUserService,
      @Value("${server.servlet.session.cookie.name:SESSION}") String sessionCookieName
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
            .requestMatchers("/ws", "/ws/**").authenticated()
            .anyRequest().denyAll()
        )
        .oauth2Login(oauth -> oauth
            .userInfoEndpoint(user -> user.oidcUserService(oidcUserService))
            .successHandler(oauth2SuccessHandler)
        )
        .logout(logout -> logout
            .logoutUrl("/auth/logout")
            .deleteCookies(sessionCookieName, requireCsrfProperties().cookieName())
            .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(NO_CONTENT)))
        .headers(headers -> headers
            .frameOptions(FrameOptionsConfig::sameOrigin)
        )
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint((request, response, exception) ->
                writeErrorResponse(response, UNAUTHORIZED.value(), UNAUTHENTICATED))
            .accessDeniedHandler((request, response, exception) ->
                writeErrorResponse(response, HttpStatus.FORBIDDEN.value(), ErrorCode.FORBIDDEN))
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
      @Value("${app.frontend.success-redirect}") String target
  ) {
    var successHandler = new SimpleUrlAuthenticationSuccessHandler(target);
    successHandler.setAlwaysUseDefaultTargetUrl(true);

    return successHandler;
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

  private static void writeErrorResponse(HttpServletResponse response, int status, ErrorCode code)
      throws IOException {
    response.setStatus(status);
    response.setContentType(APPLICATION_JSON_VALUE);
    response.getWriter().write("{\"code\":\"%s\"}".formatted(code));
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
