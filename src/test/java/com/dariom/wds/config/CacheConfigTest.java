package com.dariom.wds.config;

import static com.dariom.wds.config.CacheConfig.ALLOWED_GUESSES_CACHE;
import static com.dariom.wds.config.CacheConfig.ANSWER_WORDS_CACHE;
import static com.dariom.wds.config.CacheConfig.DISPLAY_NAME_CACHE;
import static com.dariom.wds.config.CacheConfig.USER_PROFILE_CACHE;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(CacheConfig.class)
class CacheConfigTest {

  @Autowired
  CacheManager cacheManager;

  @Test
  void cacheManager_createsExpectedCaches() {
    // Act
    var allowed = cacheManager.getCache(ALLOWED_GUESSES_CACHE);
    var answers = cacheManager.getCache(ANSWER_WORDS_CACHE);
    var displayName = cacheManager.getCache(DISPLAY_NAME_CACHE);
    var userProfile = cacheManager.getCache(USER_PROFILE_CACHE);

    // Assert
    assertThat(allowed).isNotNull();
    assertThat(answers).isNotNull();
    assertThat(displayName).isNotNull();
    assertThat(userProfile).isNotNull();
  }
}
