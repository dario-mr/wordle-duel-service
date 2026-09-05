package com.dariom.wds.persistence.entity;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static java.util.stream.Collectors.toUnmodifiableSet;

import com.dariom.wds.domain.Language;
import com.dariom.wds.domain.RoomRounds;
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

  @Enumerated(STRING)
  @Column(name = "rounds", nullable = false)
  private RoomRounds configuredRounds = RoomRounds.ENDLESS;

  @OneToMany(mappedBy = "room", cascade = ALL, orphanRemoval = true, fetch = LAZY)
  private Set<RoomPlayerEntity> roomPlayers = new HashSet<>();

  @OneToMany(mappedBy = "room", cascade = ALL, orphanRemoval = true, fetch = LAZY)
  private List<RoundEntity> rounds = new ArrayList<>();

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

  public Map<String, Integer> getMatchScoresByPlayerId() {
    var scores = new HashMap<String, Integer>();
    for (var p : roomPlayers) {
      scores.put(p.getPlayerId(), p.getMatchScore());
    }
    return scores;
  }

  public void addRound(RoundEntity round) {
    rounds.add(round);
  }

  public void setPlayerMatchScore(String playerId, Integer matchScore) {
    var normalizedScore = matchScore != null ? matchScore : 0;
    var player = getOrCreateRoomPlayer(playerId, normalizedScore);
    player.setMatchScore(normalizedScore);
  }

  public void setPlayerMatchScoreIfNotSet(String playerId, Integer matchScore) {
    if (findRoomPlayer(playerId).isPresent()) {
      return;
    }

    var normalizedScore = matchScore != null ? matchScore : 0;
    getOrCreateRoomPlayer(playerId, normalizedScore);
  }

  public void markRematchRequested(String playerId) {
    findRoomPlayer(playerId).ifPresent(player -> player.setRematchRequested(true));
  }

  public boolean allPlayersRequestedRematch() {
    return roomPlayers.size() == 2
        && roomPlayers.stream().allMatch(RoomPlayerEntity::isRematchRequested);
  }

  public void incrementPlayerMatchScore(String playerId, int delta) {
    var player = getOrCreateRoomPlayer(playerId, 0);
    player.setMatchScore(player.getMatchScore() + delta);
  }

  public void incrementWinnerWins() {
    var highestMatchScore = roomPlayers.stream()
        .mapToInt(RoomPlayerEntity::getMatchScore)
        .max()
        .orElse(0);
    var winners = roomPlayers.stream()
        .filter(player -> player.getMatchScore() == highestMatchScore)
        .toList();
    if (winners.size() == 1) {
      var winner = winners.getFirst();
      winner.setWins(winner.getWins() + 1);
    }
  }

  public void clearMatchScores() {
    roomPlayers.forEach(player -> player.setMatchScore(0));
  }

  public void resetForRematch() {
    rounds.clear();
    roomPlayers.forEach(player -> {
      player.setCurrentRoundNumber(null);
      player.setRematchRequested(false);
    });
    clearMatchScores();
    status = RoomStatus.IN_PROGRESS;
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

  public Optional<RoomPlayerEntity> findRoomPlayer(String playerId) {
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
