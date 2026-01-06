package com.dariom.wds.persistence.entity;

import static jakarta.persistence.FetchType.LAZY;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "room_players")
public class RoomPlayerEntity {

  @EmbeddedId
  private RoomPlayerIdEmbeddable id;

  @MapsId("roomId")
  @ManyToOne(fetch = LAZY)
  @JoinColumn(name = "room_id", nullable = false)
  private RoomEntity room;

  @Column(name = "score", nullable = false)
  private int score;

  public RoomPlayerEntity() {
  }

  public RoomPlayerEntity(RoomEntity room, String playerId, int score) {
    this.room = Objects.requireNonNull(room, "room");
    var roomId = Objects.requireNonNull(room.getId(), "room.id");
    this.id = new RoomPlayerIdEmbeddable(roomId, Objects.requireNonNull(playerId, "playerId"));
    this.score = score;
  }

  public String getPlayerId() {
    return id != null ? id.getPlayerId() : null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof RoomPlayerEntity other)) {
      return false;
    }

    return id != null && other.id != null && id.equals(other.id);
  }

  @Override
  public int hashCode() {
    return id != null ? id.hashCode() : 0;
  }
}

