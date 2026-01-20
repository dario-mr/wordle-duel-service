package com.dariom.wds.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.List;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

  public static final String ALLOWED_GUESSES_CACHE = "allowedGuesses";
  public static final String ANSWER_WORDS_CACHE = "answerWords";
  public static final String DISPLAY_NAME_CACHE = "userDisplayName";

  private static final Duration DICTIONARY_CACHE_TTL = Duration.ofDays(1);

  private static final Duration DISPLAY_NAME_TTL = Duration.ofDays(1);
  private static final long DISPLAY_NAME_MAX_SIZE = 50_000;

  @Bean
  public CacheManager cacheManager() {
    var manager = new SimpleCacheManager();

    var dictionaryBuilder = Caffeine.newBuilder()
        .expireAfterWrite(DICTIONARY_CACHE_TTL)
        .recordStats();

    var displayNameBuilder = Caffeine.newBuilder()
        .expireAfterWrite(DISPLAY_NAME_TTL)
        .maximumSize(DISPLAY_NAME_MAX_SIZE)
        .recordStats();

    manager.setCaches(List.of(
        new CaffeineCache(ALLOWED_GUESSES_CACHE, dictionaryBuilder.build()),
        new CaffeineCache(ANSWER_WORDS_CACHE, dictionaryBuilder.build()),
        new CaffeineCache(DISPLAY_NAME_CACHE, displayNameBuilder.build())
    ));

    return manager;
  }
}
