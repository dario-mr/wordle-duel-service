package com.dariom.wds.persistence.repository.jpa;

import com.dariom.wds.persistence.entity.RoundEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoundJpaRepository extends JpaRepository<RoundEntity, Long> {

  Optional<RoundEntity> findByRoomIdAndRoundNumber(String roomId, int roundNumber);

  List<RoundEntity> findByRoomIdOrderByRoundNumberAsc(String roomId);
}
