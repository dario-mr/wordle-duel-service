package com.dariom.wds.service.auth;

import static com.dariom.wds.config.security.SecurityProperties.JwtProperties;
import static com.dariom.wds.config.security.SecurityProperties.RefreshProperties;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.SET_COOKIE;

import com.dariom.wds.config.security.SecurityProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshTokenCookieServiceTest {

  @Mock
  private HttpServletRequest request;
  @Mock
  private HttpServletResponse response;

  private RefreshTokenCookieService service;

  @BeforeEach
  void setUp() {
    var securityProperties = new SecurityProperties(
        "", null, null,
        new JwtProperties("issuer", 900, "secret"),
        new RefreshProperties(7, "wd_refresh", "Lax", "/", false)
    );

    service = new RefreshTokenCookieService(securityProperties);
  }

  @Test
  void readRefreshToken_noCookies_returnsEmpty() {
    // Arrange
    when(request.getCookies()).thenReturn(null);

    // Act
    var result = service.readRefreshToken(request);

    // Assert
    assertThat(result).isEmpty();
    verify(request).getCookies();
  }

  @Test
  void readRefreshToken_cookiePresent_returnsValue() {
    // Arrange
    var cookies = new Cookie[]{
        new Cookie("other", "x"),
        new Cookie("wd_refresh", "token")
    };

    when(request.getCookies()).thenReturn(cookies);

    // Act
    var result = service.readRefreshToken(request);

    // Assert
    assertThat(result).isEqualTo(Optional.of("token"));
    verify(request).getCookies();
  }

  @Test
  void readRefreshToken_cookiePresentButBlank_returnsEmpty() {
    // Arrange
    var cookies = new Cookie[]{
        new Cookie("wd_refresh", " ")
    };
    when(request.getCookies()).thenReturn(cookies);

    // Act
    var result = service.readRefreshToken(request);

    // Assert
    assertThat(result).isEmpty();
    verify(request).getCookies();
  }

  @Test
  void setRefreshToken_setsExpectedCookieHeader() {
    // Arrange
    var rawToken = "raw-token";

    // Act
    service.setRefreshToken(response, rawToken);

    // Assert
    var cookieCaptor = ArgumentCaptor.forClass(String.class);
    verify(response).addHeader(eq(SET_COOKIE), cookieCaptor.capture());

    var header = cookieCaptor.getValue();
    assertThat(header).contains("wd_refresh=" + rawToken);
    assertThat(header).contains("Path=/");
    assertThat(header).contains("HttpOnly");
    assertThat(header).contains("SameSite=Lax");
    assertThat(header).contains("Max-Age=");
    assertThat(header).doesNotContain("Secure");
  }

  @Test
  void clearRefreshToken_setsExpiredCookieHeader() {
    // Act
    service.clearRefreshToken(response);

    // Assert
    var cookieCaptor = ArgumentCaptor.forClass(String.class);
    verify(response).addHeader(eq(SET_COOKIE), cookieCaptor.capture());

    var header = cookieCaptor.getValue();
    assertThat(header).contains("wd_refresh=");
    assertThat(header).contains("Path=/");
    assertThat(header).contains("HttpOnly");
    assertThat(header).contains("SameSite=Lax");
    assertThat(header).contains("Max-Age=0");
  }
}
