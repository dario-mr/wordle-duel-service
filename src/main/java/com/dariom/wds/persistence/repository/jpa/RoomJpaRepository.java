package com.dariom.wds.persistence.repository.jpa;

import com.dariom.wds.persistence.entity.RoomEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomJpaRepository extends JpaRepository<RoomEntity, String> {

  @EntityGraph(attributePaths = {
      "playerIds",
      "scoresByPlayerId",
      "rounds",
      "rounds.statusByPlayerId",
      "rounds.guesses",
      "rounds.guesses.letters"
  })
  Optional<RoomEntity> findWithDetailsById(String id);
}
