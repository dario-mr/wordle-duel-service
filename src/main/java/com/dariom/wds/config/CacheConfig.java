package com.dariom.wds.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

  public static final String ALLOWED_GUESSES_CACHE = "allowedGuesses";
  public static final String ANSWER_WORDS_CACHE = "answerWords";

  private static final Duration DICTIONARY_CACHE_TTL = Duration.ofDays(1);

  @Bean
  public CacheManager cacheManager() {
    var cacheManager = new CaffeineCacheManager(ALLOWED_GUESSES_CACHE, ANSWER_WORDS_CACHE);
    cacheManager.setCaffeine(Caffeine.newBuilder()
        .expireAfterWrite(DICTIONARY_CACHE_TTL));

    return cacheManager;
  }
}
