package com.dariom.wds.job;

import static java.time.temporal.ChronoUnit.DAYS;

import com.dariom.wds.config.RoomCleanupProperties;
import com.dariom.wds.service.room.RoomService;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class RoomCleanupJob {

  private final RoomCleanupProperties properties;
  private final RoomService roomService;
  private final Clock clock;

  @Scheduled(cron = "${room.cleanup.cron}")
  void cleanupOldRooms() {
    var cutoff = Instant.now(clock)
        .minus(properties.retentionDays(), DAYS);

    var deletedRooms = roomService.deleteRoom(cutoff);
    log.info("Deleted {} rooms older than {} (cutoff={})", deletedRooms,
        properties.retentionDays(), cutoff);
  }
}
