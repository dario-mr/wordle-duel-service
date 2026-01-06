package com.dariom.wds.persistence.entity;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.IDENTITY;

import com.dariom.wds.domain.Language;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "dictionary_words")
public class DictionaryWordEntity {

  @Id
  @GeneratedValue(strategy = IDENTITY)
  @Column(name = "id")
  private Long id;

  @Enumerated(STRING)
  @Column(name = "language")
  private Language language;

  @Column(name = "is_answer", nullable = false)
  private boolean answer;

  @Column(name = "word", nullable = false)
  private String word;

}

