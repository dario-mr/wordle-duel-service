package com.dariom.wds.persistence.repository.jpa;

import com.dariom.wds.persistence.entity.RoomEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomJpaRepository extends JpaRepository<RoomEntity, String> {

  // load relations eagerly, to avoid N+1 issue and lazy init exceptions
  @EntityGraph(attributePaths = {
      "roomPlayers"
  })
  Optional<RoomEntity> findWithPlayersAndScoresById(String id);

}
