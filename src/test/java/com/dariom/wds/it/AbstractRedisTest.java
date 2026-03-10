package com.dariom.wds.it;

import static com.dariom.wds.it.config.SharedContainers.REDIS;
import static com.dariom.wds.it.config.SharedContainers.isRedisContainerManaged;

import java.util.UUID;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

public abstract class AbstractRedisTest {

  @DynamicPropertySource
  static void registerTestProperties(DynamicPropertyRegistry registry) {
    var databaseName = "testdb_" + UUID.randomUUID().toString().replace("-", "");
    registry.add(
        "spring.datasource.url",
        () -> "jdbc:h2:mem:%s;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;INIT="
            .formatted(databaseName)
            + "CREATE SCHEMA IF NOT EXISTS wordle\\;SET SCHEMA wordle\\;"
    );

    if (isRedisContainerManaged()) {
      registry.add("spring.data.redis.host", REDIS::getHost);
      registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }
  }
}
