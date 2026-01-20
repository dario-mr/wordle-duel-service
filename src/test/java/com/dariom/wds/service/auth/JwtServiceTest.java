package com.dariom.wds.service.auth;

import static com.dariom.wds.config.security.SecurityProperties.JwtProperties;
import static java.time.ZoneOffset.UTC;
import static java.util.Map.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.dariom.wds.config.security.SecurityProperties;
import com.dariom.wds.domain.AccessToken;
import com.dariom.wds.persistence.entity.AppUserEntity;
import com.dariom.wds.persistence.entity.RoleEntity;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

  private static final Instant NOW = Instant.parse("2025-01-01T12:00:00Z");
  private final Clock clock = Clock.fixed(NOW, UTC);

  @Mock
  private JwtEncoder jwtEncoder;

  private JwtService jwtService;

  @BeforeEach
  void setUp() {
    var securityProperties = new SecurityProperties(
        "", null, null,
        new JwtProperties("test-issuer", 900, "test-secret"),
        null
    );

    jwtService = new JwtService(securityProperties, jwtEncoder, clock);
  }

  @Test
  void createAccessToken_fullNamePresent_includesNameClaimAndEncodesJwt() {
    // Arrange
    var userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var user = new AppUserEntity(userId, "user@test.com", "google-sub-1", "User Test");
    user.addRole(new RoleEntity("USER"));
    user.addRole(new RoleEntity("ADMIN"));

    when(jwtEncoder.encode(any(JwtEncoderParameters.class)))
        .thenReturn(new Jwt(
            "encoded-jwt",
            NOW,
            NOW.plusSeconds(900),
            of("alg", "HS256"),
            of("sub", "ignored")
        ));

    // Act
    var token = jwtService.createAccessToken(user);

    // Assert
    assertThat(token).isEqualTo(new AccessToken("encoded-jwt", 900));

    var captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
    verify(jwtEncoder).encode(captor.capture());
    verifyNoMoreInteractions(jwtEncoder);

    var params = captor.getValue();
    assertThat(params.getJwsHeader().getAlgorithm().getName()).isEqualTo("HS256");

    var claims = params.getClaims();
    assertThat(claims.getClaimAsString("iss")).isEqualTo("test-issuer");
    assertThat(claims.getIssuedAt()).isEqualTo(NOW);
    assertThat(claims.getExpiresAt()).isEqualTo(NOW.plusSeconds(900));
    assertThat(claims.getSubject()).isEqualTo("user@test.com");

    assertThat(claims.getClaimAsString("uid")).isEqualTo(userId.toString());
    assertThat(claims.getClaimAsStringList("roles")).containsExactly("ADMIN", "USER");
    assertThat(claims.getClaimAsString("name")).isEqualTo("User Test");
  }

  @Test
  void createAccessToken_fullNameBlank_omitsNameClaim() {
    // Arrange
    var userId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    var user = new AppUserEntity(userId, "user@test.com", "google-sub-1", "   ");

    when(jwtEncoder.encode(any(JwtEncoderParameters.class)))
        .thenReturn(new Jwt(
            "encoded-jwt",
            NOW,
            NOW.plusSeconds(900),
            of("alg", "HS256"),
            of("sub", "ignored")
        ));

    // Act
    jwtService.createAccessToken(user);

    // Assert
    var captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
    verify(jwtEncoder).encode(captor.capture());

    var claims = captor.getValue().getClaims();
    assertThat(claims.hasClaim("name")).isFalse();
  }

  @Test
  void createAccessToken_rolesUnsorted_sortsRolesClaim() {
    // Arrange
    var userId = UUID.fromString("00000000-0000-0000-0000-000000000003");
    var user = new AppUserEntity(userId, "user@test.com", "google-sub-1", "User Test");
    user.addRole(new RoleEntity("USER"));
    user.addRole(new RoleEntity("ADMIN"));

    when(jwtEncoder.encode(any(JwtEncoderParameters.class)))
        .thenReturn(new Jwt(
            "encoded-jwt",
            NOW,
            NOW.plusSeconds(900),
            of("alg", "HS256"),
            of("sub", "ignored")
        ));

    // Act
    jwtService.createAccessToken(user);

    // Assert
    var captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
    verify(jwtEncoder).encode(captor.capture());

    var claims = captor.getValue().getClaims();
    assertThat(claims.getClaimAsStringList("roles"))
        .containsExactly("ADMIN", "USER");
  }
}
