package com.dariom.wds.persistence.repository.jpa;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;

import com.dariom.wds.persistence.entity.RefreshTokenEntity;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenEntity, UUID> {

  @EntityGraph(attributePaths = "user")
  @Lock(PESSIMISTIC_WRITE)
  Optional<RefreshTokenEntity> findWithUserByTokenHash(String tokenHash);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("delete from RefreshTokenEntity rt where rt.tokenHash = :tokenHash")
  int deleteByTokenHash(@Param("tokenHash") String tokenHash);

  // TODO index candidate, monitor performance
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("delete from RefreshTokenEntity rt where rt.expiresAt < :now")
  int deleteExpired(@Param("now") Instant now);
}
