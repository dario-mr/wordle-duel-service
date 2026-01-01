package com.dariom.wds.persistence.entity;

import com.dariom.wds.domain.DictionaryWordType;
import com.dariom.wds.domain.Language;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "dictionary_words")
public class DictionaryWordEntity {

  public DictionaryWordEntity() {
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  private Language language;

  @Enumerated(EnumType.STRING)
  private DictionaryWordType type;

  @Column(nullable = false)
  private String word;

}

