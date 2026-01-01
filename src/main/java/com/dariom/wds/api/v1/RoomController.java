package com.dariom.wds.api.v1;

import com.dariom.wds.api.v1.dto.CreateRoomRequest;
import com.dariom.wds.api.v1.dto.GuessResponse;
import com.dariom.wds.api.v1.dto.JoinRoomRequest;
import com.dariom.wds.api.v1.dto.RoomDto;
import com.dariom.wds.api.v1.dto.SubmitGuessRequest;
import com.dariom.wds.api.v1.mapper.RoomMapper;
import com.dariom.wds.service.GameService;
import com.dariom.wds.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
public class RoomController {

  private final RoomService roomService;
  private final GameService gameService;
  private final RoomMapper roomMapper;

  @PostMapping
  public ResponseEntity<RoomDto> createRoom(@RequestBody CreateRoomRequest request) {
    var room = roomService.createRoom(request.language(), request.playerId());
    return ResponseEntity.ok(roomMapper.toDto(room));
  }

  @PostMapping("/{roomId}/join")
  public ResponseEntity<RoomDto> joinRoom(
      @PathVariable String roomId,
      @RequestBody JoinRoomRequest request
  ) {
    var room = roomService.joinRoom(roomId, request.playerId());
    return ResponseEntity.ok(roomMapper.toDto(room));
  }

  @GetMapping("/{roomId}")
  public ResponseEntity<RoomDto> getRoom(@PathVariable String roomId) {
    var room = roomService.getRoom(roomId);
    return ResponseEntity.ok(roomMapper.toDto(room));
  }

  @PostMapping("/{roomId}/guess")
  public ResponseEntity<GuessResponse> submitGuess(
      @PathVariable String roomId,
      @RequestBody SubmitGuessRequest request
  ) {
    var room = gameService.handleGuess(roomId, request.playerId(), request.word());
    return ResponseEntity.ok(new GuessResponse(roomMapper.toDto(room)));
  }

}
