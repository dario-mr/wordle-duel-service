package com.dariom.wds.service;

import com.dariom.wds.domain.LetterStatus;
import com.dariom.wds.persistence.entity.LetterResultEmbeddable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class WordleEvaluator {
  // todo review

  public List<LetterResultEmbeddable> evaluate(String targetWord, String guessWord) {
    if (targetWord.length() != guessWord.length()) {
      throw new IllegalArgumentException("targetWord and guessWord must have same length");
    }

    var target = targetWord.toUpperCase(Locale.ROOT);
    var guess = guessWord.toUpperCase(Locale.ROOT);

    var results = new ArrayList<LetterResultEmbeddable>(guess.length());
    for (int i = 0; i < guess.length(); i++) {
      results.add(new LetterResultEmbeddable(guess.charAt(i), LetterStatus.ABSENT));
    }

    Map<Character, Integer> remaining = new HashMap<>();
    for (int i = 0; i < target.length(); i++) {
      remaining.merge(target.charAt(i), 1, Integer::sum);
    }

    for (int i = 0; i < target.length(); i++) {
      if (guess.charAt(i) == target.charAt(i)) {
        results.set(i, new LetterResultEmbeddable(guess.charAt(i), LetterStatus.CORRECT));
        remaining.merge(target.charAt(i), -1, Integer::sum);
      }
    }

    for (int i = 0; i < target.length(); i++) {
      if (results.get(i).getStatus() == LetterStatus.CORRECT) {
        continue;
      }

      char letter = guess.charAt(i);
      int count = remaining.getOrDefault(letter, 0);
      if (count > 0) {
        results.set(i, new LetterResultEmbeddable(letter, LetterStatus.PRESENT));
        remaining.put(letter, count - 1);
      } else {
        results.set(i, new LetterResultEmbeddable(letter, LetterStatus.ABSENT));
      }
    }

    return results;
  }
}
