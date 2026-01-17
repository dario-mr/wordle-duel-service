package com.dariom.wds.persistence.entity;

import static jakarta.persistence.FetchType.LAZY;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "refresh_token")
public class RefreshTokenEntity {

  @Id
  @Column(name = "id", nullable = false)
  private UUID id;

  @ManyToOne(fetch = LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private AppUserEntity user;

  @Column(name = "token_hash", nullable = false, unique = true, length = 64)
  private String tokenHash;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  protected RefreshTokenEntity() {
  }

  public RefreshTokenEntity(UUID id, AppUserEntity user, String tokenHash, Instant createdAt,
      Instant expiresAt) {
    this.id = id;
    this.user = user;
    this.tokenHash = tokenHash;
    this.createdAt = createdAt;
    this.expiresAt = expiresAt;
  }
}
