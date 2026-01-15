package com.dariom.wds.service.auth;

import static com.dariom.wds.config.security.SecurityProperties.JwtProperties;
import static com.dariom.wds.config.security.SecurityProperties.RefreshProperties;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.dariom.wds.config.security.SecurityProperties;
import com.dariom.wds.domain.AccessToken;
import com.dariom.wds.exception.InvalidRefreshTokenException;
import com.dariom.wds.persistence.entity.AppUserEntity;
import com.dariom.wds.persistence.entity.RefreshTokenEntity;
import com.dariom.wds.persistence.repository.jpa.RefreshTokenJpaRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

  private static final Instant NOW = Instant.parse("2025-01-01T12:00:00Z");
  private static final int REFRESH_TOKEN_DURATION_DAYS = 7;

  private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

  @Mock
  private RefreshTokenGenerator refreshTokenGenerator;
  @Mock
  private TokenHashing tokenHashing;
  @Mock
  private RefreshTokenJpaRepository refreshTokenRepository;
  @Mock
  private JwtService jwtService;

  private RefreshTokenService service;

  @BeforeEach
  void setUp() {
    var securityProperties = new SecurityProperties(
        "",
        new JwtProperties("issuer", 900, "secret"),
        new RefreshProperties(REFRESH_TOKEN_DURATION_DAYS, "wd_refresh", "Lax", "/", false)
    );

    service = new RefreshTokenService(
        securityProperties,
        refreshTokenGenerator,
        tokenHashing,
        refreshTokenRepository,
        jwtService,
        clock
    );
  }

  @Test
  void createRefreshToken_validUser_savesHashedTokenAndReturnsRawToken() {
    // Arrange
    var user = new AppUserEntity(UUID.randomUUID(), "user@test.com", "google-sub-1", "User Test");
    var rawToken = "raw-token";
    var tokenHash = "hash";

    when(refreshTokenGenerator.generate()).thenReturn(rawToken);
    when(tokenHashing.sha256Hex(anyString())).thenReturn(tokenHash);

    // Act
    var created = service.createRefreshToken(user);

    // Assert
    assertThat(created).isEqualTo(rawToken);

    verify(refreshTokenGenerator).generate();
    verify(tokenHashing).sha256Hex(rawToken);

    var captor = ArgumentCaptor.forClass(RefreshTokenEntity.class);
    verify(refreshTokenRepository).save(captor.capture());

    var saved = captor.getValue();
    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getUser()).isEqualTo(user);
    assertThat(saved.getTokenHash()).isEqualTo(tokenHash);
    assertThat(saved.getCreatedAt()).isEqualTo(NOW);
    assertThat(saved.getExpiresAt())
        .isEqualTo(NOW.plusSeconds(REFRESH_TOKEN_DURATION_DAYS * 24L * 60L * 60L));

    verifyNoMoreInteractions(refreshTokenGenerator, tokenHashing, refreshTokenRepository,
        jwtService);
  }

  @Test
  void refresh_validToken_deletesOldCreatesNewAndReturnsResult() {
    // Arrange
    var user = new AppUserEntity(UUID.randomUUID(), "user@test.com", "google-sub-1", "User Test");
    var inputRawToken = "old-raw";
    var existingHash = "old-hash";
    var newRawToken = "new-raw";
    var newHash = "new-hash";

    var existing = new RefreshTokenEntity(
        UUID.randomUUID(),
        user,
        existingHash,
        NOW.minusSeconds(60),
        NOW.plusSeconds(60)
    );

    when(tokenHashing.sha256Hex(anyString())).thenReturn(existingHash, newHash);
    when(refreshTokenRepository.findByTokenHash(existingHash)).thenReturn(Optional.of(existing));
    when(refreshTokenGenerator.generate()).thenReturn(newRawToken);
    when(jwtService.createAccessToken(any(AppUserEntity.class)))
        .thenReturn(new AccessToken("access", 900));

    // Act
    var result = service.refresh(inputRawToken);

    // Assert
    assertThat(result.refreshToken()).isEqualTo(newRawToken);
    assertThat(result.accessToken()).isEqualTo(new AccessToken("access", 900));

    verify(tokenHashing).sha256Hex(inputRawToken);
    verify(refreshTokenRepository).findByTokenHash(existingHash);
    verify(refreshTokenRepository).delete(existing);

    verify(refreshTokenGenerator).generate();
    verify(tokenHashing).sha256Hex(newRawToken);

    var saveCaptor = ArgumentCaptor.forClass(RefreshTokenEntity.class);
    verify(refreshTokenRepository).save(saveCaptor.capture());

    var saved = saveCaptor.getValue();
    assertThat(saved.getUser()).isEqualTo(user);
    assertThat(saved.getTokenHash()).isEqualTo(newHash);
    assertThat(saved.getCreatedAt()).isEqualTo(NOW);

    verify(jwtService).createAccessToken(user);
    verifyNoMoreInteractions(refreshTokenGenerator, tokenHashing, refreshTokenRepository,
        jwtService);
  }

  @Test
  void refresh_expiredToken_deletesAndThrows() {
    // Arrange
    var user = new AppUserEntity(UUID.randomUUID(), "user@test.com", "google-sub-1", "User Test");
    var inputRawToken = "old-raw";
    var existingHash = "old-hash";

    var existing = new RefreshTokenEntity(
        UUID.randomUUID(),
        user,
        existingHash,
        NOW.minusSeconds(60),
        NOW
    );

    when(tokenHashing.sha256Hex(anyString())).thenReturn(existingHash);
    when(refreshTokenRepository.findByTokenHash(existingHash)).thenReturn(Optional.of(existing));

    // Act
    var thrown = catchThrowable(() -> service.refresh(inputRawToken));

    // Assert
    assertThat(thrown).isInstanceOf(InvalidRefreshTokenException.class);

    verify(tokenHashing).sha256Hex(inputRawToken);
    verify(refreshTokenRepository).findByTokenHash(existingHash);
    verify(refreshTokenRepository).delete(existing);
    verifyNoMoreInteractions(refreshTokenGenerator, tokenHashing, refreshTokenRepository,
        jwtService);
  }

  @Test
  void refresh_unknownToken_throwsAndDoesNotDelete() {
    // Arrange
    var inputRawToken = "old-raw";
    var existingHash = "old-hash";

    when(tokenHashing.sha256Hex(anyString())).thenReturn(existingHash);
    when(refreshTokenRepository.findByTokenHash(existingHash)).thenReturn(Optional.empty());

    // Act
    var thrown = catchThrowable(() -> service.refresh(inputRawToken));

    // Assert
    assertThat(thrown).isInstanceOf(InvalidRefreshTokenException.class);

    verify(tokenHashing).sha256Hex(inputRawToken);
    verify(refreshTokenRepository).findByTokenHash(existingHash);
    verifyNoMoreInteractions(refreshTokenGenerator, tokenHashing, refreshTokenRepository,
        jwtService);
  }

  @Test
  void revoke_existingToken_deletes() {
    // Arrange
    var rawToken = "raw-token";
    var tokenHash = "hash";
    var existing = new RefreshTokenEntity(
        UUID.randomUUID(),
        new AppUserEntity(UUID.randomUUID(), "user@test.com", "google-sub-1", "User Test"),
        tokenHash,
        NOW.minusSeconds(60),
        NOW.plusSeconds(60)
    );

    when(tokenHashing.sha256Hex(anyString())).thenReturn(tokenHash);
    when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(existing));

    // Act
    service.revoke(rawToken);

    // Assert
    verify(tokenHashing).sha256Hex(rawToken);
    verify(refreshTokenRepository).findByTokenHash(tokenHash);
    verify(refreshTokenRepository).delete(existing);
    verifyNoMoreInteractions(refreshTokenGenerator, tokenHashing, refreshTokenRepository,
        jwtService);
  }

  @Test
  void revoke_unknownToken_doesNothing() {
    // Arrange
    var rawToken = "raw-token";
    var tokenHash = "hash";

    when(tokenHashing.sha256Hex(anyString())).thenReturn(tokenHash);
    when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.empty());

    // Act
    service.revoke(rawToken);

    // Assert
    verify(tokenHashing).sha256Hex(rawToken);
    verify(refreshTokenRepository).findByTokenHash(tokenHash);
    verifyNoMoreInteractions(refreshTokenGenerator, tokenHashing, refreshTokenRepository,
        jwtService);
  }
}
