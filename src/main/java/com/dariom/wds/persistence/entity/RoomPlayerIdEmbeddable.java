package com.dariom.wds.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Embeddable
public class RoomPlayerIdEmbeddable implements Serializable {

  @Column(name = "room_id")
  private String roomId;

  @Column(name = "player_id")
  private String playerId;

  public RoomPlayerIdEmbeddable() {
  }

  public RoomPlayerIdEmbeddable(String roomId, String playerId) {
    this.roomId = roomId;
    this.playerId = playerId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RoomPlayerIdEmbeddable that = (RoomPlayerIdEmbeddable) o;
    return Objects.equals(roomId, that.roomId) && Objects.equals(playerId, that.playerId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(roomId, playerId);
  }
}
