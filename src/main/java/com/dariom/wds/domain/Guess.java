package com.dariom.wds.domain;

import java.util.List;

public record Guess(
    String word,
    List<LetterResult> letters,
    int attemptNumber
) {

}
