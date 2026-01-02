package com.dariom.wds.persistence.entity;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;

import com.dariom.wds.domain.Language;
import com.dariom.wds.domain.RoomStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "rooms")
public class RoomEntity {

  @Id
  @Column(name = "id")
  private String id;

  @Enumerated(STRING)
  @Column(name = "language")
  private Language language;

  @Enumerated(STRING)
  @Column(name = "status")
  private RoomStatus status;

  @ElementCollection(fetch = LAZY)
  @CollectionTable(name = "room_players", joinColumns = @JoinColumn(name = "room_id"))
  @Column(name = "player_id")
  private Set<String> playerIds = new HashSet<>();

  @ElementCollection(fetch = LAZY)
  @CollectionTable(name = "room_scores", joinColumns = @JoinColumn(name = "room_id"))
  @MapKeyColumn(name = "player_id")
  @Column(name = "score")
  private Map<String, Integer> scoresByPlayerId = new HashMap<>();

  @OneToMany(mappedBy = "room", cascade = ALL, orphanRemoval = true, fetch = LAZY)
  private List<RoundEntity> rounds = new ArrayList<>();

  @Column(name = "current_round_number")
  private Integer currentRoundNumber;

  @Column(name = "created_at")
  private Instant createdAt;

  @Column(name = "last_updated_at")
  private Instant lastUpdatedAt;

  public RoomEntity() {
  }

  public void addPlayer(String playerId) {
    playerIds.add(playerId);
  }

  public void addRound(RoundEntity round) {
    rounds.add(round);
  }

  public void setPlayerScore(String playerId, Integer score) {
    scoresByPlayerId.put(playerId, score);
  }

  public void setPlayerScoreIfNotSet(String playerId, Integer score) {
    scoresByPlayerId.putIfAbsent(playerId, score);
  }

  public List<String> getSortedPlayerIds() {
    return playerIds.stream().sorted().toList();
  }

  @PrePersist
  void prePersist() {
    var now = Instant.now();
    if (createdAt == null) {
      createdAt = now;
    }
    lastUpdatedAt = now;
  }

  @PreUpdate
  void preUpdate() {
    lastUpdatedAt = Instant.now();
  }

}
