package com.dariom.wds.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class RoomLockManagerTest {

  private final RoomLockManager roomLockManager = new RoomLockManager();

  @Test
  void withRoomLock_givenSameRoomId_serializesAccess() throws Exception {
    // Arrange
    var inCriticalSection = new AtomicInteger(0);
    var maxConcurrentInCriticalSection = new AtomicInteger(0);

    var threads = 16;
    var tasks = 200;

    var startLatch = new CountDownLatch(1);
    var doneLatch = new CountDownLatch(tasks);

    // Act
    try (var executor = Executors.newFixedThreadPool(threads)) {
      for (int i = 0; i < tasks; i++) {
        executor.submit(() -> {
          try {
            assertThat(startLatch.await(5, TimeUnit.SECONDS)).isTrue();

            roomLockManager.withRoomLock("room-1", () -> {
              var current = inCriticalSection.incrementAndGet();
              maxConcurrentInCriticalSection.getAndUpdate(prev -> Math.max(prev, current));

              try {
                Thread.sleep(2);
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
              } finally {
                inCriticalSection.decrementAndGet();
              }

              return null;
            });
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          } finally {
            doneLatch.countDown();
          }
        });
      }

      startLatch.countDown();
      assertThat(doneLatch.await(10, TimeUnit.SECONDS)).isTrue();
    }

    // Assert
    assertThat(maxConcurrentInCriticalSection.get()).isEqualTo(1);
  }

  @Test
  void withRoomLock_givenManyRoomIds_doesNotLeakLocks() {
    // Arrange
    var iterations = 2_000;

    // Act
    for (int i = 0; i < iterations; i++) {
      var roomId = "room-" + i;
      roomLockManager.withRoomLock(roomId, () -> null);
    }

    // Assert
    assertThat(roomLockManager.registeredLockCount())
        .withFailMessage("Expected lock registry to be empty after use")
        .isZero();
  }
}
