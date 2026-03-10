package com.dariom.wds.it.config;

import static org.testcontainers.containers.wait.strategy.Wait.forListeningPort;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

public final class SharedContainers {

  public static final PostgreSQLContainer<?> POSTGRES;
  public static final GenericContainer<?> REDIS;

  static {
    if (System.getenv("SPRING_DATASOURCE_URL") == null) {
      POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
          .withUsername("test")
          .withPassword("test")
          .withDatabaseName("postgres")
          .waitingFor(forListeningPort());
      POSTGRES.start();
    } else {
      POSTGRES = null;
    }

    if (System.getenv("SPRING_DATA_REDIS_HOST") == null) {
      REDIS = new GenericContainer<>("redis:7-alpine")
          .withExposedPorts(6379)
          .waitingFor(forListeningPort());
      REDIS.start();
    } else {
      REDIS = null;
    }
  }

  private SharedContainers() {
  }

  public static boolean isPostgresContainerManaged() {
    return POSTGRES != null;
  }

  public static boolean isRedisContainerManaged() {
    return REDIS != null;
  }
}
