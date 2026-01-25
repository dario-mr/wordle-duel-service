package com.dariom.wds.persistence.repository.jpa;

import com.dariom.wds.persistence.entity.RoundEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoundJpaRepository extends JpaRepository<RoundEntity, Long> {

  @EntityGraph(attributePaths = {
      "statusByPlayerId",
      "guesses",
      "guesses.letters"
  })
  Optional<RoundEntity> findWithDetailsByRoomIdAndRoundNumber(String roomId, int roundNumber);

  @EntityGraph(attributePaths = {
      "statusByPlayerId",
      "guesses",
      "guesses.letters"
  })
  @Query("""
      select r
      from RoundEntity r
      join r.room room
      where room.id in :roomIds
        and room.currentRoundNumber is not null
        and r.roundNumber = room.currentRoundNumber
      """)
  List<RoundEntity> findCurrentRoundsWithDetailsByRoomIds(@Param("roomIds") List<String> roomIds);

}
