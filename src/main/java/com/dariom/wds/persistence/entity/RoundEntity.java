package com.dariom.wds.persistence.entity;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;

import com.dariom.wds.domain.RoundPlayerStatus;
import com.dariom.wds.domain.RoundStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "rounds")
public class RoundEntity {

  @Id
  @GeneratedValue(strategy = IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "round_number")
  private int roundNumber;

  @ManyToOne(fetch = LAZY)
  @JoinColumn(name = "room_id", nullable = false)
  private RoomEntity room;

  @Column(name = "target_word")
  private String targetWord;

  @Column(name = "max_attempts")
  private int maxAttempts;

  @ElementCollection(fetch = LAZY)
  @CollectionTable(name = "round_player_status", joinColumns = @JoinColumn(name = "round_id"))
  @MapKeyColumn(name = "player_id")
  @Column(name = "status")
  @Enumerated(STRING)
  private Map<String, RoundPlayerStatus> statusByPlayerId = new HashMap<>();

  @Column(name = "status", nullable = false)
  @Enumerated(STRING)
  private RoundStatus roundStatus;

  @Column(name = "started_at")
  private Instant startedAt;

  @Column(name = "finished_at")
  private Instant finishedAt;

  @OneToMany(mappedBy = "round", cascade = ALL, orphanRemoval = true, fetch = LAZY)
  @OrderColumn(name = "guess_order")
  private List<GuessEntity> guesses = new ArrayList<>();

  public RoundEntity() {
  }

  public void setPlayerStatus(String playerId, RoundPlayerStatus roundPlayerStatus) {
    statusByPlayerId.put(playerId, roundPlayerStatus);
  }

  public RoundPlayerStatus getPlayerStatus(String playerId) {
    return statusByPlayerId.get(playerId);
  }

  public void addGuess(GuessEntity guessEntity) {
    guesses.add(guessEntity);
  }

  public int currentAttemptNumber(String playerId) {
    return (int) guesses.stream()
        .filter(g -> Objects.equals(g.getPlayerId(), playerId))
        .count();
  }

  public int nextAttemptNumber(String playerId) {
    return currentAttemptNumber(playerId) + 1;
  }
}
