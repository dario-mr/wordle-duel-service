package com.dariom.wds.persistence.repository;

import static com.dariom.wds.domain.DictionaryWordType.ALLOWED;
import static com.dariom.wds.domain.DictionaryWordType.ANSWER;
import static java.util.stream.Collectors.toUnmodifiableSet;

import com.dariom.wds.domain.DictionaryWordType;
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
    return getWordsByLanguageAndType(language, ALLOWED);
  }

  public Set<String> getAnswerWords(Language language) {
    return getWordsByLanguageAndType(language, ANSWER);
  }

  private Set<String> getWordsByLanguageAndType(Language language, DictionaryWordType wordType) {
    return jpaRepository.findByLanguageAndType(language, wordType).stream()
        .map(dictionaryWord -> dictionaryWord.getWord().toUpperCase())
        .collect(toUnmodifiableSet());
  }

}
