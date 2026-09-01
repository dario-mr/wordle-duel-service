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
      select distinct r
      from RoundEntity r
      join r.room room
      join room.roomPlayers player
      where room.id in :roomIds
        and player.id.playerId = :playerId
        and player.currentRoundNumber is not null
        and r.roundNumber = player.currentRoundNumber
      """)
  List<RoundEntity> findCurrentRoundsWithDetailsByRoomIdsAndPlayerId(
      @Param("roomIds") List<String> roomIds, @Param("playerId") String playerId);

  @EntityGraph(attributePaths = {
      "statusByPlayerId",
      "guesses",
      "guesses.letters"
  })
  @Query("""
      select r
      from RoundEntity r
      join r.room room
      join room.roomPlayers player
      where room.id = :roomId
        and player.id.playerId = :playerId
        and player.currentRoundNumber is not null
        and r.roundNumber = player.currentRoundNumber
      """)
  Optional<RoundEntity> findCurrentRoundWithDetailsByRoomIdAndPlayerId(
      @Param("roomId") String roomId, @Param("playerId") String playerId);

}
