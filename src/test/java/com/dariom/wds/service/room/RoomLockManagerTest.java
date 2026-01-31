package com.dariom.wds.service.room;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.dariom.wds.config.lock.RoomLockProperties;
import com.dariom.wds.exception.RoomLockedException;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.integration.support.locks.DefaultLockRegistry;

class RoomLockManagerTest {

  private final DefaultLockRegistry lockRegistry = new DefaultLockRegistry();
  private final RoomLockManager roomLockManager = new RoomLockManager(lockRegistry,
      new RoomLockProperties(true, Duration.ofSeconds(3), Duration.ofSeconds(60)));

  @Test
  void withRoomLock_sameRoomId_serializesAccess() throws Exception {
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
            assertThat(startLatch.await(5, SECONDS)).isTrue();

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
            });
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          } finally {
            doneLatch.countDown();
          }
        });
      }

      startLatch.countDown();
      assertThat(doneLatch.await(10, SECONDS)).isTrue();
    }

    // Assert
    assertThat(maxConcurrentInCriticalSection.get()).isEqualTo(1);
  }

  @Test
  void withRoomLockRunnable_validRunnable_executesRunnable() {
    // Arrange
    var calls = new AtomicInteger(0);

    // Act
    roomLockManager.withRoomLock("room-1", calls::incrementAndGet);

    // Assert
    assertThat(calls.get()).isEqualTo(1);
  }

  @Test
  void withRoomLock_lockedTimeout_throwsRoomLockedException() {
    // Arrange
    var fastFailRoomLockManager = new RoomLockManager(lockRegistry,
        new RoomLockProperties(true, Duration.ofMillis(50), Duration.ofSeconds(60)));

    var lock = lockRegistry.obtain("room:room-1");
    var lockedLatch = new CountDownLatch(1);
    var releaseLatch = new CountDownLatch(1);

    try (var executor = Executors.newSingleThreadExecutor()) {
      executor.submit(() -> {
        lock.lock();
        lockedLatch.countDown();
        try {
          assertThat(releaseLatch.await(5, SECONDS)).isTrue();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        } finally {
          lock.unlock();
        }
      });

      assertThat(lockedLatch.await(5, SECONDS)).isTrue();

      // Act
      var thrown = catchThrowable(
          () -> fastFailRoomLockManager.withRoomLock("room-1", () -> true));

      // Assert
      assertThat(thrown).isInstanceOf(RoomLockedException.class);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    } finally {
      releaseLatch.countDown();
    }
  }
}
