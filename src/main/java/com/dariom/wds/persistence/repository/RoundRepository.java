package com.dariom.wds.persistence.repository;

import com.dariom.wds.persistence.entity.RoundEntity;
import com.dariom.wds.persistence.repository.jpa.RoundJpaRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RoundRepository {

  private final RoundJpaRepository roundJpaRepository;

  public Optional<RoundEntity> findWithDetailsByRoomIdAndRoundNumber(
      String roomId, int roundNumber) {
    return roundJpaRepository.findWithDetailsByRoomIdAndRoundNumber(roomId, roundNumber);
  }
}
