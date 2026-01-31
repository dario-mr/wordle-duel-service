package com.dariom.wds.config.lock;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.integration.support.locks.DefaultLockRegistry;
import org.springframework.integration.support.locks.LockRegistry;

@Configuration
@RequiredArgsConstructor
class RoomLockRegistryConfig {

  private static final String REGISTRY_KEY_PREFIX = "wordle-duel-service:room-lock";

  private final RoomLockProperties properties;

  @Bean
  @ConditionalOnProperty(prefix = "room.lock", name = "enabled", havingValue = "true")
  LockRegistry roomLockRegistry(RedisConnectionFactory redisConnectionFactory) {
    return new RedisLockRegistry(
        redisConnectionFactory,
        REGISTRY_KEY_PREFIX,
        properties.expire().toMillis()
    );
  }

  @Bean
  @ConditionalOnProperty(prefix = "room.lock", name = "enabled", havingValue = "false")
  LockRegistry localRoomLockRegistry() {
    return new DefaultLockRegistry();
  }
}
