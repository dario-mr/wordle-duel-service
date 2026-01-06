package com.dariom.wds.config;

import static com.dariom.wds.config.CacheConfig.ALLOWED_GUESSES_CACHE;
import static com.dariom.wds.config.CacheConfig.ANSWER_WORDS_CACHE;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CacheConfigTest {

  @Test
  void cacheManager_createsExpectedCaches() {
    // Arrange
    var cacheManager = new CacheConfig().cacheManager();

    // Act
    var allowed = cacheManager.getCache(ALLOWED_GUESSES_CACHE);
    var answers = cacheManager.getCache(ANSWER_WORDS_CACHE);

    // Assert
    assertThat(allowed).isNotNull();
    assertThat(answers).isNotNull();
  }
}
