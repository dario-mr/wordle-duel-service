package com.dariom.wds.persistence.repository;

import com.dariom.wds.domain.DictionaryWordType;
import com.dariom.wds.domain.Language;
import com.dariom.wds.persistence.repository.jpa.DictionaryWordJpaRepository;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class DictionaryRepository {

  private final DictionaryWordJpaRepository jpaRepository;

  public DictionaryRepository(DictionaryWordJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  public Set<String> getAllowedGuesses(Language language) {
    return jpaRepository.findByLanguageAndType(language, DictionaryWordType.ALLOWED)
        .stream()
        .map(e -> e.getWord().toUpperCase())
        .collect(Collectors.toUnmodifiableSet());
  }

  public Set<String> getAnswerWords(Language language) {
    return jpaRepository.findByLanguageAndType(language, DictionaryWordType.ANSWER)
        .stream()
        .map(e -> e.getWord().toUpperCase())
        .collect(Collectors.toUnmodifiableSet());
  }
}
