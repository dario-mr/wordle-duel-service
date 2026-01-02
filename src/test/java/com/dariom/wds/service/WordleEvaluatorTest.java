package com.dariom.wds.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.dariom.wds.domain.LetterStatus;
import org.junit.jupiter.api.Test;

class WordleEvaluatorTest {

  private final WordleEvaluator evaluator = new WordleEvaluator();

  @Test
  void marksAllCorrectWhenGuessEqualsTarget() {
    var res = evaluator.evaluate("PIZZA", "PIZZA");

    assertThat(res).hasSize(5);
    assertThat(res.stream().allMatch(r -> r.getStatus() == LetterStatus.CORRECT)).isTrue();
  }

  @Test
  void handlesDuplicateLettersCorrectly_case1() {
    var res = evaluator.evaluate("MAMMA", "AMMMA");

    assertThat(res).hasSize(5);
    assertThat(res.get(0).getStatus()).isEqualTo(LetterStatus.PRESENT);
    assertThat(res.get(1).getStatus()).isEqualTo(LetterStatus.PRESENT);
    assertThat(res.get(2).getStatus()).isEqualTo(LetterStatus.CORRECT);
    assertThat(res.get(3).getStatus()).isEqualTo(LetterStatus.CORRECT);
    assertThat(res.get(4).getStatus()).isEqualTo(LetterStatus.CORRECT);
  }

  @Test
  void handlesDuplicateLettersCorrectly_case2() {
    var res = evaluator.evaluate("PIZZA", "ZZZZZ");

    assertThat(res).hasSize(5);
    assertThat(res.get(0).getStatus()).isEqualTo(LetterStatus.ABSENT);
    assertThat(res.get(1).getStatus()).isEqualTo(LetterStatus.ABSENT);
    assertThat(res.get(2).getStatus()).isEqualTo(LetterStatus.CORRECT);
    assertThat(res.get(3).getStatus()).isEqualTo(LetterStatus.CORRECT);
    assertThat(res.get(4).getStatus()).isEqualTo(LetterStatus.ABSENT);
  }
}
