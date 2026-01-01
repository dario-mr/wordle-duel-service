package com.dariom.wds.persistence.entity;

import com.dariom.wds.domain.RoundPlayerStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "rounds")
public class RoundEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private int roundNumber;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "room_id", nullable = false)
  private RoomEntity room;

  private String targetWord;

  private int maxAttempts;

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name = "round_player_status", joinColumns = @JoinColumn(name = "round_id"))
  @MapKeyColumn(name = "player_id")
  @Column(name = "status")
  @Enumerated(EnumType.STRING)
  private Map<String, RoundPlayerStatus> statusByPlayerId = new HashMap<>();

  private boolean finished;

  private Instant startedAt;

  private Instant finishedAt;

  @OneToMany(mappedBy = "round", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  @OrderColumn(name = "guess_order")
  private List<GuessEntity> guesses = new ArrayList<>();

  public RoundEntity() {
  }

}
