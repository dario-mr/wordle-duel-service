package com.dariom.wds.persistence.entity;

import static jakarta.persistence.FetchType.LAZY;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

@Setter
@Getter
@Entity
@Table(name = "refresh_token")
public class RefreshTokenEntity implements Persistable<UUID> {

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

  @Transient
  private boolean persisted;

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

  @Override
  public boolean isNew() {
    return !persisted;
  }

  @PostPersist
  @PostLoad
  void markPersisted() {
    persisted = true;
  }
}
