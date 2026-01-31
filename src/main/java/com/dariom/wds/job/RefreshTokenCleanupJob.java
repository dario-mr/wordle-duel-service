package com.dariom.wds.job;

import com.dariom.wds.service.auth.RefreshTokenService;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupJob {

  private final RefreshTokenService refreshTokenService;
  private final Clock clock;

  @Scheduled(cron = "${refresh-token.cleanup.cron}")
  @SchedulerLock(name = "refreshTokenCleanupJob", lockAtMostFor = "PT15M")
  public void cleanupExpiredTokens() {
    var now = Instant.now(clock);
    var deleted = refreshTokenService.deleteExpiredTokens(now);
    log.info("Deleted {} expired refresh tokens", deleted);
  }
}
