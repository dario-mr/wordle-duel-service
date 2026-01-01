package com.dariom.wds.persistence.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "guesses")
public class GuessEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "round_id", nullable = false)
  private RoundEntity round;

  private String playerId;

  private String word;

  private int attemptNumber;

  private Instant createdAt;

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name = "guess_letters", joinColumns = @JoinColumn(name = "guess_id"))
  @OrderColumn(name = "letter_order")
  private List<LetterResultEmbeddable> letters = new ArrayList<>();

  public GuessEntity() {
  }

}
