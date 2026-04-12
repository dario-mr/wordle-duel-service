package com.dariom.wds.persistence.repository.jpa;

import com.dariom.wds.persistence.entity.RoundEntity;
import com.dariom.wds.persistence.repository.jpa.projection.GuessLetterView;
import com.dariom.wds.persistence.repository.jpa.projection.RoundHeaderView;
import com.dariom.wds.persistence.repository.jpa.projection.RoundStatusView;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoundReadJpaRepository extends JpaRepository<RoundEntity, Long> {

  @Query("""
      select new com.dariom.wds.persistence.repository.jpa.projection.RoundHeaderView(
        r.id,
        room.id,
        r.roundNumber,
        r.maxAttempts,
        r.roundStatus,
        r.targetWord
      )
      from RoundEntity r
      join r.room room
      where room.id = :roomId
        and r.roundNumber = :roundNumber
      """)
  Optional<RoundHeaderView> findRoundHeaderByRoomIdAndRoundNumber(
      @Param("roomId") String roomId,
      @Param("roundNumber") int roundNumber
  );

  @Query("""
      select new com.dariom.wds.persistence.repository.jpa.projection.RoundHeaderView(
        r.id,
        room.id,
        r.roundNumber,
        r.maxAttempts,
        r.roundStatus,
        r.targetWord
      )
      from RoundEntity r
      join r.room room
      where room.id in :roomIds
        and room.currentRoundNumber is not null
        and r.roundNumber = room.currentRoundNumber
      """)
  List<RoundHeaderView> findCurrentRoundHeadersByRoomIds(@Param("roomIds") List<String> roomIds);

  @Query("""
      select new com.dariom.wds.persistence.repository.jpa.projection.RoundStatusView(
        r.id,
        key(statusEntry),
        value(statusEntry)
      )
      from RoundEntity r
      join r.statusByPlayerId statusEntry
      where r.id in :roundIds
      """)
  List<RoundStatusView> findRoundStatusesByRoundIds(@Param("roundIds") Collection<Long> roundIds);

  @Query("""
      select new com.dariom.wds.persistence.repository.jpa.projection.GuessLetterView(
        r.id,
        g.id,
        g.playerId,
        g.word,
        g.attemptNumber,
        index(letterResult),
        letterResult.letter,
        letterResult.status
      )
      from RoundEntity r
      join r.guesses g
      left join g.letters letterResult
      where r.id in :roundIds
      order by r.id, g.playerId, g.attemptNumber, index(letterResult)
      """)
  List<GuessLetterView> findGuessLettersByRoundIds(@Param("roundIds") Collection<Long> roundIds);
}
