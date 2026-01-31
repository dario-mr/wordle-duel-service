package com.dariom.wds.it;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import com.dariom.wds.config.lock.RoomLockProperties;
import com.dariom.wds.exception.RoomLockedException;
import com.dariom.wds.service.room.RoomLockManager;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class RoomLockRedisIT {

  @Container
  static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
      .withExposedPorts(6379)
      .waitingFor(Wait.forListeningPort());

  @Test
  void withRoomLock_twoInstances_onlyOneExecutes() throws Exception {
    // Arrange
    var connectionFactory = new LettuceConnectionFactory(redis.getHost(),
        redis.getMappedPort(6379));
    connectionFactory.afterPropertiesSet();

    try {
      var registry1 = new RedisLockRegistry(connectionFactory, "room-lock-it", 60_000);
      var registry2 = new RedisLockRegistry(connectionFactory, "room-lock-it", 60_000);

      var properties = new RoomLockProperties(true, Duration.ofMillis(50), Duration.ofSeconds(60));
      var manager1 = new RoomLockManager(registry1, properties);
      var manager2 = new RoomLockManager(registry2, properties);

      var start = new CountDownLatch(1);
      var lockAcquired = new CountDownLatch(1);
      var executor = Executors.newFixedThreadPool(2);

      var success = new AtomicReference<String>();
      var failure = new AtomicReference<Throwable>();

      try {
        var f1 = executor.submit(() -> {
          await(start);
          manager1.withRoomLock("room-1", () -> {
            lockAcquired.countDown();
            sleep(250);
            success.compareAndSet(null, "manager1");
            return true;
          });
        });

        var f2 = executor.submit(() -> {
          await(start);
          await(lockAcquired);
          try {
            manager2.withRoomLock("room-1", () -> {
              success.compareAndSet(null, "manager2");
              return true;
            });
          } catch (Throwable t) {
            failure.compareAndSet(null, t);
          }
        });

        start.countDown();
        f1.get(5, SECONDS);
        f2.get(5, SECONDS);
      } finally {
        executor.shutdownNow();
      }

      // Assert
      assertThat(success.get()).isEqualTo("manager1");
      assertThat(failure.get()).isInstanceOf(RoomLockedException.class);
    } finally {
      connectionFactory.destroy();
    }
  }

  private static void await(CountDownLatch latch) {
    try {
      assertThat(latch.await(5, SECONDS)).isTrue();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }

  private static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }
}
