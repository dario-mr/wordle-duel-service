package com.dariom.wds.it;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import jakarta.annotation.Resource;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Import(ShedLockRedisIT.TestBeans.class)
@Testcontainers(disabledWithoutDocker = true)
class ShedLockRedisIT {

  @Resource
  private LockedTestJob lockedTestJob;

  @Resource
  private AtomicInteger counter;

  @Container
  static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
      .withExposedPorts(6379)
      .waitingFor(Wait.forListeningPort());

  @DynamicPropertySource
  static void registerRedisProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.redis.host", redis::getHost);
    registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    registry.add("spring.session.store-type", () -> "none");
  }

  @Test
  void schedulerLock_allowsOnlyOneConcurrentExecution() throws Exception {
    var start = new CountDownLatch(1);
    var executor = Executors.newFixedThreadPool(2);

    try {
      var f1 = executor.submit(() -> {
        await(start);
        lockedTestJob.run();
      });

      var f2 = executor.submit(() -> {
        await(start);
        lockedTestJob.run();
      });

      start.countDown();
      f1.get(10, SECONDS);
      f2.get(10, SECONDS);
    } finally {
      executor.shutdownNow();
    }

    assertThat(counter.get()).isEqualTo(1);
  }

  private static void await(CountDownLatch latch) {
    try {
      latch.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }

  @TestConfiguration
  static class TestBeans {

    @Bean
    AtomicInteger counter() {
      return new AtomicInteger();
    }

    @Bean
    LockedTestJob lockedTestJob(AtomicInteger counter) {
      return new LockedTestJob(counter);
    }
  }

  static class LockedTestJob {

    private final AtomicInteger counter;

    LockedTestJob(AtomicInteger counter) {
      this.counter = counter;
    }

    @SchedulerLock(name = "shedLockRedisIT", lockAtMostFor = "PT5S", lockAtLeastFor = "PT2S")
    public void run() {
      counter.incrementAndGet();

      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException(e);
      }
    }
  }
}
