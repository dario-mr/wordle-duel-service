package com.dariom.wds.config.security;

import static org.springframework.util.StringUtils.hasText;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.function.Supplier;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;

public class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {

  private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
  private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      Supplier<CsrfToken> csrfToken
  ) {
    this.xor.handle(request, response, csrfToken);
    // Force deferred CSRF token to load so CookieCsrfTokenRepository writes/refreshes XSRF-TOKEN.
    csrfToken.get();
  }

  @Override
  public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
    var headerValue = request.getHeader(csrfToken.getHeaderName());
    // SPA sends the raw cookie value in the header; only use XOR decoding when needed.
    return (hasText(headerValue) ? this.plain : this.xor)
        .resolveCsrfTokenValue(request, csrfToken);
  }
}
