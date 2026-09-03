package com.dariom.wds.persistence.entity;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.IDENTITY;

import com.dariom.wds.domain.RoomMessagePreset;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "room_messages")
public class RoomMessageEntity {

  @Id
  @GeneratedValue(strategy = IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "room_id", nullable = false)
  private String roomId;

  @Column(name = "sender_player_id", nullable = false)
  private String senderPlayerId;

  @Enumerated(STRING)
  @Column(name = "preset", nullable = false)
  private RoomMessagePreset preset;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;
}
