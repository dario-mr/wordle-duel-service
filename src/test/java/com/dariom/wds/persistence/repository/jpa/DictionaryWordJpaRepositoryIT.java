package com.dariom.wds.persistence.repository.jpa;

import static com.dariom.wds.domain.Language.IT;
import static org.assertj.core.api.Assertions.assertThat;

import com.dariom.wds.persistence.entity.DictionaryWordEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@JpaRepositoryIT
class DictionaryWordJpaRepositoryIT {

  @Autowired
  private DictionaryWordJpaRepository repository;

  @Test
  void findByLanguage_returnsAllWordsForLanguage() {
    // Arrange
    repository.save(word("pizza", true));
    repository.save(word("fuoco", false));

    // Act
    var results = repository.findByLanguage(IT);

    // Assert
    assertThat(results)
        .extracting(DictionaryWordEntity::getWord)
        .containsExactlyInAnyOrder("pizza", "fuoco");
  }

  @Test
  void findByLanguageAndAnswerTrue_returnsOnlyAnswerWords() {
    // Arrange
    repository.save(word("pizza", true));
    repository.save(word("fuoco", false));

    // Act
    var results = repository.findByLanguageAndAnswerTrue(IT);

    // Assert
    assertThat(results)
        .extracting(DictionaryWordEntity::getWord)
        .containsExactly("pizza");
  }

  private static DictionaryWordEntity word(String word, boolean isAnswer) {
    var entity = new DictionaryWordEntity();
    entity.setWord(word);
    entity.setAnswer(isAnswer);
    entity.setLanguage(IT);

    return entity;
  }
}
