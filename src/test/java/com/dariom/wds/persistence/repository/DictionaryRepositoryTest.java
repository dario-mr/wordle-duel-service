package com.dariom.wds.persistence.repository;

import static com.dariom.wds.config.CacheConfig.ALLOWED_GUESSES_CACHE;
import static com.dariom.wds.config.CacheConfig.ANSWER_WORDS_CACHE;
import static com.dariom.wds.domain.Language.IT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dariom.wds.config.CacheConfig;
import com.dariom.wds.persistence.entity.DictionaryWordEntity;
import com.dariom.wds.persistence.repository.jpa.DictionaryWordJpaRepository;
import jakarta.annotation.Resource;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = {CacheConfig.class, DictionaryRepository.class})
class DictionaryRepositoryTest {

  @MockitoBean
  private DictionaryWordJpaRepository jpaRepository;

  @Resource
  private DictionaryRepository repository;

  @Resource
  private CacheManager cacheManager;

  @BeforeEach
  void setUp() {
    var allowedGuessesCache = cacheManager.getCache(ALLOWED_GUESSES_CACHE);
    if (allowedGuessesCache != null) {
      allowedGuessesCache.clear();
    }

    var answerWordsCache = cacheManager.getCache(ANSWER_WORDS_CACHE);
    if (answerWordsCache != null) {
      answerWordsCache.clear();
    }
  }

  @Test
  void getAllowedGuesses_givenSameLanguage_returnsCachedResult() {
    // Arrange
    when(jpaRepository.findByLanguage(IT))
        .thenReturn(List.of(word("pizza"), word("fuoco")));

    // Act
    var first = repository.getAllowedGuesses(IT);
    var second = repository.getAllowedGuesses(IT);

    // Assert
    assertThat(first).containsExactlyInAnyOrder("PIZZA", "FUOCO");
    assertThat(first).isSameAs(second);
    verify(jpaRepository).findByLanguage(IT);
  }

  @Test
  void getAnswerWords_givenSameLanguage_returnsCachedResult() {
    // Arrange
    when(jpaRepository.findByLanguageAndAnswerTrue(IT))
        .thenReturn(List.of(word("pizza")));

    // Act
    var first = repository.getAnswerWords(IT);
    var second = repository.getAnswerWords(IT);

    // Assert
    assertThat(first).containsExactly("PIZZA");
    assertThat(first).isSameAs(second);
    verify(jpaRepository).findByLanguageAndAnswerTrue(IT);
  }

  @Test
  void getAllowedGuessesAndAnswerWords_givenSameLanguage_usesSeparateCaches() {
    // Arrange
    when(jpaRepository.findByLanguage(IT))
        .thenReturn(List.of(word("pizza"), word("fuoco")));
    when(jpaRepository.findByLanguageAndAnswerTrue(IT))
        .thenReturn(List.of(word("pizza")));

    // Act
    repository.getAllowedGuesses(IT);
    repository.getAnswerWords(IT);
    repository.getAllowedGuesses(IT);
    repository.getAnswerWords(IT);

    // Assert
    verify(jpaRepository).findByLanguage(IT);
    verify(jpaRepository).findByLanguageAndAnswerTrue(IT);
  }

  private static DictionaryWordEntity word(String value) {
    var entity = new DictionaryWordEntity();
    entity.setLanguage(IT);
    entity.setWord(value);
    return entity;
  }
}
