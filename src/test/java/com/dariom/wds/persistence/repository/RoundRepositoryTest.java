package com.dariom.wds.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dariom.wds.persistence.entity.RoundEntity;
import com.dariom.wds.persistence.repository.jpa.RoundJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoundRepositoryTest {

  @Mock
  private RoundJpaRepository roundJpaRepository;

  @InjectMocks
  private RoundRepository roundRepository;

  @Test
  void findWithDetailsByRoomIdAndRoundNumber_roundExists_delegatesToJpaRepository() {
    // Arrange
    var round = new RoundEntity();
    when(roundJpaRepository.findWithDetailsByRoomIdAndRoundNumber("room-1", 2))
        .thenReturn(Optional.of(round));

    // Act
    var found = roundRepository.findWithDetailsByRoomIdAndRoundNumber("room-1", 2);

    // Assert
    assertThat(found).containsSame(round);
    verify(roundJpaRepository).findWithDetailsByRoomIdAndRoundNumber("room-1", 2);
  }
}
