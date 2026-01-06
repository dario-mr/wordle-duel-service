package com.dariom.wds.persistence.entity;

import static jakarta.persistence.EnumType.STRING;

import com.dariom.wds.domain.LetterStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Embeddable
public class LetterResultEmbeddable {

  @Column(name = "letter")
  private char letter;

  @Enumerated(STRING)
  @Column(name = "status")
  private LetterStatus status;

  public LetterResultEmbeddable() {
  }

  public LetterResultEmbeddable(char letter, LetterStatus status) {
    this.letter = letter;
    this.status = status;
  }

}
