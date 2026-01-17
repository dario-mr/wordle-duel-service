package com.dariom.wds.persistence.repository.jpa;

import com.dariom.wds.persistence.entity.AppUserEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserJpaRepository extends JpaRepository<AppUserEntity, UUID> {

  @EntityGraph(attributePaths = "roles")
  Optional<AppUserEntity> findByEmail(String email);

  @EntityGraph(attributePaths = "roles")
  Optional<AppUserEntity> findByGoogleSub(String googleSub);
}
