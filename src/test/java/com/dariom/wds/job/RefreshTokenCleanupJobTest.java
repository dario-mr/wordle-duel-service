package com.dariom.wds.job;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dariom.wds.service.auth.RefreshTokenService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshTokenCleanupJobTest {

  @Mock
  private RefreshTokenService refreshTokenService;

  @Test
  void cleanupExpiredTokens_deletesExpiredTokensUpToNow() {
    // Arrange
    var now = Instant.parse("2025-01-10T00:00:00Z");
    var clock = Clock.fixed(now, ZoneOffset.UTC);
    var job = new RefreshTokenCleanupJob(refreshTokenService, clock);

    when(refreshTokenService.deleteExpiredTokens(now)).thenReturn(3);

    // Act
    job.cleanupExpiredTokens();

    // Assert
    verify(refreshTokenService).deleteExpiredTokens(now);
  }
}
