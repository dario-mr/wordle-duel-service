package com.dariom.wds.persistence.repository.jpa;

import com.dariom.wds.persistence.entity.RoundEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoundJpaRepository extends JpaRepository<RoundEntity, Long> {

  @EntityGraph(attributePaths = {
      "statusByPlayerId",
      "guesses",
      "guesses.letters"
  })
  Optional<RoundEntity> findWithDetailsByRoomIdAndRoundNumber(String roomId, int roundNumber);

}
