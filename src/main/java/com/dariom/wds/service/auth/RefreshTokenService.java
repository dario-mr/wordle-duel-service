package com.dariom.wds.service.auth;

import static java.time.temporal.ChronoUnit.DAYS;

import com.dariom.wds.config.security.SecurityProperties;
import com.dariom.wds.domain.RefreshResult;
import com.dariom.wds.exception.InvalidRefreshTokenException;
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
    var now = Instant.now(clock);
    var hashedToken = tokenHashing.sha256Hex(rawToken);
    var existingToken = refreshTokenRepository.findByTokenHash(hashedToken)
        .orElseThrow(InvalidRefreshTokenException::new);

    if (!existingToken.getExpiresAt().isAfter(now)) {
      refreshTokenRepository.delete(existingToken);
      throw new InvalidRefreshTokenException();
    }

    var user = existingToken.getUser();

    refreshTokenRepository.delete(existingToken);

    var refreshToken = createRefreshToken(user);
    var accessToken = jwtService.createAccessToken(user);

    return new RefreshResult(refreshToken, accessToken);
  }

  @Transactional
  public void revoke(String rawToken) {
    var hashedToken = tokenHashing.sha256Hex(rawToken);
    refreshTokenRepository.findByTokenHash(hashedToken)
        .ifPresent(refreshTokenRepository::delete);
  }

  @Transactional
  public int deleteExpiredTokens(Instant now) {
    return refreshTokenRepository.deleteExpired(now);
  }
}
