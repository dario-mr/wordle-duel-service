package com.dariom.wds.persistence.entity;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
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
  @GeneratedValue(strategy = IDENTITY)
  @Column(name = "id")
  private Long id;

  @ManyToOne(fetch = LAZY)
  @JoinColumn(name = "round_id", nullable = false)
  private RoundEntity round;

  @Column(name = "player_id")
  private String playerId;

  @Column(name = "word")
  private String word;

  @Column(name = "attempt_number")
  private int attemptNumber;

  @Column(name = "created_at", updatable = false)
  private Instant createdAt;

  @ElementCollection(fetch = LAZY)
  @CollectionTable(name = "guess_letters", joinColumns = @JoinColumn(name = "guess_id"))
  @OrderColumn(name = "letter_order")
  private List<LetterResultEmbeddable> letters = new ArrayList<>();

  public GuessEntity() {
  }

}
