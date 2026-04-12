package com.dariom.wds.persistence.repository.jpa;

import com.dariom.wds.persistence.entity.RoundEntity;
import com.dariom.wds.persistence.repository.jpa.projection.RoundFlatRowView;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoundReadJpaRepository extends JpaRepository<RoundEntity, Long> {

  @Query("""
      select new com.dariom.wds.persistence.repository.jpa.projection.RoundFlatRowView(
        r.id,
        room.id,
        r.roundNumber,
        r.maxAttempts,
        r.roundStatus,
        r.targetWord,
        key(statusEntry),
        value(statusEntry),
        coalesce(g.id, -1L),
        g.playerId,
        g.word,
        g.attemptNumber,
        index(letterResult),
        letterResult.letter,
        letterResult.status
      )
      from RoundEntity r
      join r.room room
      left join r.statusByPlayerId statusEntry
      left join r.guesses g
      left join g.letters letterResult
      where room.id = :roomId
        and r.roundNumber = :roundNumber
      order by key(statusEntry), g.playerId, g.attemptNumber, index(letterResult)
      """)
  List<RoundFlatRowView> findFlatRowsByRoomIdAndRoundNumber(
      @Param("roomId") String roomId,
      @Param("roundNumber") int roundNumber
  );

  @Query("""
      select new com.dariom.wds.persistence.repository.jpa.projection.RoundFlatRowView(
        r.id,
        room.id,
        r.roundNumber,
        r.maxAttempts,
        r.roundStatus,
        r.targetWord,
        key(statusEntry),
        value(statusEntry),
        coalesce(g.id, -1L),
        g.playerId,
        g.word,
        g.attemptNumber,
        index(letterResult),
        letterResult.letter,
        letterResult.status
      )
      from RoundEntity r
      join r.room room
      left join r.statusByPlayerId statusEntry
      left join r.guesses g
      left join g.letters letterResult
      where room.id in :roomIds
        and room.currentRoundNumber is not null
        and r.roundNumber = room.currentRoundNumber
      order by room.id, key(statusEntry), g.playerId, g.attemptNumber, index(letterResult)
      """)
  List<RoundFlatRowView> findCurrentFlatRowsByRoomIds(@Param("roomIds") List<String> roomIds);
}
