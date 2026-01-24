package com.dariom.wds.job;

import static java.time.temporal.ChronoUnit.DAYS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dariom.wds.config.RoomCleanupProperties;
import com.dariom.wds.service.room.RoomService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoomCleanupJobTest {

  @Mock
  private RoomService roomService;

  @Test
  void cleanupInactiveRooms_validConfig_deletesRoomsOlderThanRetentionDays() {
    // Arrange
    var now = Instant.parse("2025-01-10T00:00:00Z");
    var properties = new RoomCleanupProperties(60);
    var clock = Clock.fixed(now, ZoneOffset.UTC);
    var job = new RoomCleanupJob(properties, roomService, clock);

    when(roomService.deleteInactiveRooms(any())).thenReturn(2L);

    // Act
    job.cleanupInactiveRooms();

    // Assert
    var expectedCutoff = now.minus(60, DAYS);
    verify(roomService).deleteInactiveRooms(expectedCutoff);
  }
}

