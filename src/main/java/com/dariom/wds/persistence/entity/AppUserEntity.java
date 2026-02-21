package com.dariom.wds.persistence.entity;

import static jakarta.persistence.FetchType.EAGER;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Setter
@Getter
@Entity
@Table(name = "app_user")
public class AppUserEntity {

  @Id
  @Column(name = "id", nullable = false)
  private UUID id;

  @Column(name = "email", nullable = false, unique = true, length = 320)
  private String email;

  @Column(name = "google_sub", nullable = false, unique = true, length = 64)
  private String googleSub;

  @Column(name = "full_name", length = 128)
  private String fullName;

  @Column(name = "picture_url", length = 512)
  private String pictureUrl;

  @Column(name = "display_name", length = 32)
  private String displayName;

  @CreationTimestamp
  @Column(name = "created_on", nullable = false, updatable = false)
  private Instant createdOn;

  @ManyToMany(fetch = EAGER)
  @JoinTable(
      name = "app_user_role",
      joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"),
      inverseJoinColumns = @JoinColumn(name = "role_name", referencedColumnName = "name")
  )
  private Set<RoleEntity> roles = new HashSet<>();

  protected AppUserEntity() {
  }

  public AppUserEntity(UUID id, String email, String googleSub, String fullName,
      String pictureUrl) {
    this.id = id;
    this.email = email;
    this.googleSub = googleSub;
    this.fullName = fullName;
    this.pictureUrl = pictureUrl;
  }

  public void addRole(RoleEntity role) {
    roles.add(role);
  }
}
