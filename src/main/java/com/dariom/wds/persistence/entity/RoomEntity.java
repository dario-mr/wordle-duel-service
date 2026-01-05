package com.dariom.wds.persistence.entity;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static java.util.stream.Collectors.toUnmodifiableSet;

import com.dariom.wds.domain.Language;
import com.dariom.wds.domain.RoomStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
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
import java.util.Objects;
import java.util.Optional;
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

  @OneToMany(mappedBy = "room", cascade = ALL, orphanRemoval = true, fetch = LAZY)
  private Set<RoomPlayerEntity> roomPlayers = new HashSet<>();

  @OneToMany(mappedBy = "room", cascade = ALL, orphanRemoval = true, fetch = LAZY)
  private List<RoundEntity> rounds = new ArrayList<>();

  @Column(name = "current_round_number")
  private Integer currentRoundNumber;

  @Column(name = "created_at", updatable = false)
  private Instant createdAt;

  @Column(name = "last_updated_at")
  private Instant lastUpdatedAt;

  public RoomEntity() {
  }

  public void addPlayer(String playerId) {
    getOrCreateRoomPlayer(playerId, 0);
  }

  public Set<String> getPlayerIds() {
    return roomPlayers.stream()
        .map(RoomPlayerEntity::getPlayerId)
        .collect(toUnmodifiableSet());
  }

  public Map<String, Integer> getScoresByPlayerId() {
    var scores = new HashMap<String, Integer>();
    for (var p : roomPlayers) {
      scores.put(p.getPlayerId(), p.getScore());
    }
    return scores;
  }

  public void addRound(RoundEntity round) {
    rounds.add(round);
  }

  public void setPlayerScore(String playerId, Integer score) {
    var normalizedScore = score != null ? score : 0;
    var player = getOrCreateRoomPlayer(playerId, normalizedScore);
    player.setScore(normalizedScore);
  }

  public void setPlayerScoreIfNotSet(String playerId, Integer score) {
    if (findRoomPlayer(playerId).isPresent()) {
      return;
    }

    var normalizedScore = score != null ? score : 0;
    getOrCreateRoomPlayer(playerId, normalizedScore);
  }

  public void incrementPlayerScore(String playerId, int delta) {
    var player = getOrCreateRoomPlayer(playerId, 0);
    player.setScore(player.getScore() + delta);
  }

  public List<String> getSortedPlayerIds() {
    return getPlayerIds().stream().sorted().toList();
  }

  private RoomPlayerEntity getOrCreateRoomPlayer(String playerId, int initialScore) {
    var existing = findRoomPlayer(playerId);
    if (existing.isPresent()) {
      return existing.get();
    }

    var created = new RoomPlayerEntity(this, playerId, initialScore);
    roomPlayers.add(created);
    return created;
  }

  private Optional<RoomPlayerEntity> findRoomPlayer(String playerId) {
    for (var player : roomPlayers) {
      if (Objects.equals(player.getPlayerId(), playerId)) {
        return Optional.of(player);
      }
    }
    return Optional.empty();
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
