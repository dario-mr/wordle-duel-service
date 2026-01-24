package com.dariom.wds.persistence.repository.jpa;

import com.dariom.wds.persistence.entity.RoomEntity;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomJpaRepository extends JpaRepository<RoomEntity, String> {

  // load relations eagerly, to avoid N+1 issue and lazy init exceptions
  @EntityGraph(attributePaths = {"roomPlayers"})
  Optional<RoomEntity> findWithPlayersAndScoresById(String id);

  // TODO index candidate, monitor performance
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("delete from RoomEntity r where r.lastUpdatedAt < :cutoff")
  long deleteInactive(@Param("cutoff") Instant cutoff);

}
