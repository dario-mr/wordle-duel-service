package com.dariom.wds.persistence.repository;

import static com.dariom.wds.domain.Language.IT;
import static org.assertj.core.api.Assertions.assertThat;

import com.dariom.wds.persistence.entity.DictionaryWordEntity;
import com.dariom.wds.persistence.repository.jpa.DictionaryWordJpaRepository;
import com.dariom.wds.persistence.repository.jpa.JpaRepositoryIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@JpaRepositoryIT
@Import(DictionaryRepository.class)
class DictionaryRepositoryJpaIT {

  @Autowired
  private DictionaryRepository repository;

  @Autowired
  private DictionaryWordJpaRepository jpaRepository;

  @Test
  void getAllowedGuesses_returnsAllWordsUppercased() {
    // Arrange
    jpaRepository.save(word("pizza", true));
    jpaRepository.save(word("fuoco", false));

    // Act
    var guesses = repository.getAllowedGuesses(IT);

    // Assert
    assertThat(guesses).containsExactlyInAnyOrder("PIZZA", "FUOCO");
  }

  @Test
  void getAnswerWords_returnsOnlyAnswerWordsUppercased() {
    // Arrange
    jpaRepository.save(word("pizza", true));
    jpaRepository.save(word("fuoco", false));

    // Act
    var answers = repository.getAnswerWords(IT);

    // Assert
    assertThat(answers).containsExactly("PIZZA");
  }

  private static DictionaryWordEntity word(String word, boolean isAnswer) {
    var entity = new DictionaryWordEntity();
    entity.setLanguage(IT);
    entity.setWord(word);
    entity.setAnswer(isAnswer);
    return entity;
  }
}
