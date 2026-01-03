package com.dariom.wds.service;

import com.dariom.wds.domain.Room;
import com.dariom.wds.service.round.RoundService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GameService {

  private final RoomLockManager roomLockManager;
  private final RoundService roundService;

  public Room handleGuess(String roomId, String playerId, String guess) {
    return roomLockManager.withRoomLock(
        roomId, () -> roundService.handleGuess(roomId, playerId, guess)
    );
  }
}
