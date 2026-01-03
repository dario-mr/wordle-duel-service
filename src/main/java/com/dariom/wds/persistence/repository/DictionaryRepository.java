package com.dariom.wds.persistence.repository;

import static java.util.stream.Collectors.toUnmodifiableSet;

import com.dariom.wds.domain.Language;
import com.dariom.wds.persistence.repository.jpa.DictionaryWordJpaRepository;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DictionaryRepository {
  // TODO add cache, these values do not change often

  private final DictionaryWordJpaRepository jpaRepository;

  public Set<String> getAllowedGuesses(Language language) {
    return jpaRepository.findByLanguage(language).stream()
        .map(dictionaryWord -> dictionaryWord.getWord().toUpperCase())
        .collect(toUnmodifiableSet());
  }

  public Set<String> getAnswerWords(Language language) {
    return jpaRepository.findByLanguageAndAnswerTrue(language).stream()
        .map(dictionaryWord -> dictionaryWord.getWord().toUpperCase())
        .collect(toUnmodifiableSet());
  }
}
