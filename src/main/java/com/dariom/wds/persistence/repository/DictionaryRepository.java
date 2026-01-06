package com.dariom.wds.persistence.repository;

import static com.dariom.wds.config.CacheConfig.ALLOWED_GUESSES_CACHE;
import static com.dariom.wds.config.CacheConfig.ANSWER_WORDS_CACHE;
import static java.util.stream.Collectors.toUnmodifiableSet;

import com.dariom.wds.domain.Language;
import com.dariom.wds.persistence.repository.jpa.DictionaryWordJpaRepository;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DictionaryRepository {

  private final DictionaryWordJpaRepository jpaRepository;

  @Cacheable(cacheNames = ALLOWED_GUESSES_CACHE, key = "#language")
  public Set<String> getAllowedGuesses(Language language) {
    return jpaRepository.findByLanguage(language).stream()
        .map(dictionaryWord -> dictionaryWord.getWord().toUpperCase())
        .collect(toUnmodifiableSet());
  }

  @Cacheable(cacheNames = ANSWER_WORDS_CACHE, key = "#language")
  public Set<String> getAnswerWords(Language language) {
    return jpaRepository.findByLanguageAndAnswerTrue(language).stream()
        .map(dictionaryWord -> dictionaryWord.getWord().toUpperCase())
        .collect(toUnmodifiableSet());
  }
}
