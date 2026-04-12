package com.dariom.wds.service.auth;

import static java.time.temporal.ChronoUnit.DAYS;

import com.dariom.wds.config.security.SecurityProperties;
import com.dariom.wds.domain.RefreshResult;
import com.dariom.wds.exception.InvalidRefreshTokenException;
import com.dariom.wds.metrics.HotPathMetrics;
import com.dariom.wds.persistence.entity.AppUserEntity;
import com.dariom.wds.persistence.entity.RefreshTokenEntity;
import com.dariom.wds.persistence.repository.jpa.RefreshTokenJpaRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

  private final SecurityProperties securityProperties;
  private final RefreshTokenGenerator refreshTokenGenerator;
  private final TokenHashing tokenHashing;
  private final RefreshTokenJpaRepository refreshTokenRepository;
  private final JwtService jwtService;
  private final Clock clock;
  private final HotPathMetrics hotPathMetrics;

  @Transactional
  public String createRefreshToken(AppUserEntity user) {
    var rawToken = refreshTokenGenerator.generate();
    var hashedToken = tokenHashing.sha256Hex(rawToken);
    var now = Instant.now(clock);
    var expiresAt = now.plus(securityProperties.refresh().ttlDays(), DAYS);

    refreshTokenRepository.save(new RefreshTokenEntity(
        UUID.randomUUID(), user, hashedToken, now, expiresAt
    ));

    return rawToken;
  }

  @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
  public RefreshResult refresh(String rawToken) {
    return hotPathMetrics.record("refresh_token.refresh", "total", () -> {
      var now = Instant.now(clock);
      var hashedToken = tokenHashing.sha256Hex(rawToken);
      var existingToken = hotPathMetrics.record("refresh_token.refresh", "load_token", () ->
          refreshTokenRepository.findWithUserByTokenHash(hashedToken)
              .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token not found in DB"))
      );

      if (!existingToken.getExpiresAt().isAfter(now)) {
        hotPathMetrics.record("refresh_token.refresh", "delete_expired_token",
            () -> refreshTokenRepository.delete(existingToken));
        throw new InvalidRefreshTokenException("Refresh token expired");
      }

      var user = existingToken.getUser();

      hotPathMetrics.record("refresh_token.refresh", "delete_existing_token",
          () -> refreshTokenRepository.delete(existingToken));

      var refreshToken = hotPathMetrics.record("refresh_token.refresh", "create_refresh_token",
          () -> createRefreshToken(user));
      var accessToken = hotPathMetrics.record("refresh_token.refresh", "create_access_token",
          () -> jwtService.createAccessToken(user));

      return new RefreshResult(refreshToken, accessToken);
    });
  }

  @Transactional
  public void revoke(String rawToken) {
    var hashedToken = tokenHashing.sha256Hex(rawToken);
    refreshTokenRepository.findWithUserByTokenHash(hashedToken)
        .ifPresent(refreshTokenRepository::delete);
  }

  @Transactional
  public int deleteExpiredTokens(Instant now) {
    return refreshTokenRepository.deleteExpired(now);
  }
}
