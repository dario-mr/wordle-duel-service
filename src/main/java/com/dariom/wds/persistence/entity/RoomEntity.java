package com.dariom.wds.persistence.entity;

import com.dariom.wds.domain.Language;
import com.dariom.wds.domain.RoomStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
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
  private String id;

  @Enumerated(EnumType.STRING)
  private Language language;

  @Enumerated(EnumType.STRING)
  private RoomStatus status;

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name = "room_players", joinColumns = @JoinColumn(name = "room_id"))
  @Column(name = "player_id")
  private Set<String> playerIds = new HashSet<>();

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name = "room_scores", joinColumns = @JoinColumn(name = "room_id"))
  @MapKeyColumn(name = "player_id")
  @Column(name = "score")
  private Map<String, Integer> scoresByPlayerId = new HashMap<>();

  @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  private List<RoundEntity> rounds = new ArrayList<>();

  private Integer currentRoundNumber;

  private Instant createdAt;

  private Instant lastUpdatedAt;

  public RoomEntity() {
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
