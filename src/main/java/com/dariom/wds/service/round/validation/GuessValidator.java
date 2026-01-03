package com.dariom.wds.service.round.validation;

import static com.dariom.wds.api.v1.error.ErrorCode.INVALID_CHARS;
import static com.dariom.wds.api.v1.error.ErrorCode.INVALID_LANGUAGE;
import static com.dariom.wds.api.v1.error.ErrorCode.INVALID_LENGTH;
import static com.dariom.wds.api.v1.error.ErrorCode.WORD_NOT_ALLOWED;

import com.dariom.wds.config.WordleProperties;
import com.dariom.wds.domain.Language;
import com.dariom.wds.exception.InvalidGuessException;
import com.dariom.wds.persistence.repository.DictionaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GuessValidator {

  private final WordleProperties properties;
  private final DictionaryRepository dictionaryRepository;

  public void validateGuess(String guess, String targetWord, Language language) {
    validateLength(guess, targetWord);
    validateGuessFormat(guess);
    validateGuessAllowed(guess, language);
  }

  private void validateLength(String guess, String targetWord) {
    if (targetWord.length() != guess.length()) {
      throw new InvalidGuessException(INVALID_LENGTH,
          "Target word and guess word must have same length");
    }
  }

  private void validateGuessFormat(String guess) {
    if (guess.length() != properties.wordLength()) {
      throw new InvalidGuessException(INVALID_LENGTH,
          "Word must be %s characters".formatted(properties.wordLength()));
    }

    for (int i = 0; i < guess.length(); i++) {
      char c = guess.charAt(i);
      if (c < 'A' || c > 'Z') {
        throw new InvalidGuessException(INVALID_CHARS, "Word must contain only letters A-Z");
      }
    }
  }

  private void validateGuessAllowed(String guess, Language language) {
    if (language == null) {
      throw new InvalidGuessException(INVALID_LANGUAGE, "Room language not set");
    }

    var allowed = dictionaryRepository.getAllowedGuesses(language);
    if (!allowed.contains(guess)) {
      throw new InvalidGuessException(WORD_NOT_ALLOWED, "Word is not in dictionary");
    }
  }
}
