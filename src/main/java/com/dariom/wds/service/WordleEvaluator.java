package com.dariom.wds.service;

import static com.dariom.wds.domain.LetterStatus.ABSENT;
import static com.dariom.wds.domain.LetterStatus.CORRECT;
import static com.dariom.wds.domain.LetterStatus.PRESENT;
import static java.util.Locale.ROOT;

import com.dariom.wds.domain.LetterResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class WordleEvaluator {

  public List<LetterResult> evaluate(String targetWord, String guessWord) {
    var target = targetWord.toUpperCase(ROOT);
    var guess = guessWord.toUpperCase(ROOT);

    var results = initAbsentResults(guess);
    var remainingCounts = countLetters(target);

    for (var i = 0; i < target.length(); i++) {
      var targetChar = target.charAt(i);
      var guessChar = guess.charAt(i);
      if (guessChar == targetChar) {
        results.set(i, new LetterResult(guessChar, CORRECT));
        remainingCounts.merge(targetChar, -1, Integer::sum);
      }
    }

    for (var i = 0; i < target.length(); i++) {
      if (results.get(i).status() == CORRECT) {
        continue;
      }

      var guessChar = guess.charAt(i);
      var count = remainingCounts.getOrDefault(guessChar, 0);
      if (count > 0) {
        results.set(i, new LetterResult(guessChar, PRESENT));
        remainingCounts.put(guessChar, count - 1);
      }
    }

    return results;
  }

  private static List<LetterResult> initAbsentResults(String guessUpper) {
    var results = new ArrayList<LetterResult>(guessUpper.length());
    for (var i = 0; i < guessUpper.length(); i++) {
      results.add(new LetterResult(guessUpper.charAt(i), ABSENT));
    }
    return results;
  }

  private static Map<Character, Integer> countLetters(String word) {
    var counts = new HashMap<Character, Integer>();
    for (var i = 0; i < word.length(); i++) {
      counts.merge(word.charAt(i), 1, Integer::sum);
    }
    return counts;
  }
}
