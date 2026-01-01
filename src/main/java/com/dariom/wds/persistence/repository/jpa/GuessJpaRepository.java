package com.dariom.wds.persistence.repository.jpa;

import com.dariom.wds.persistence.entity.GuessEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuessJpaRepository extends JpaRepository<GuessEntity, Long> {

  List<GuessEntity> findByRoundIdOrderByAttemptNumberAsc(Long roundId);

  List<GuessEntity> findByRoundIdAndPlayerIdOrderByAttemptNumberAsc(Long roundId, String playerId);
}
