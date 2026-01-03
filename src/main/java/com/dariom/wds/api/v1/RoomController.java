package com.dariom.wds.api.v1;

import com.dariom.wds.api.v1.dto.CreateRoomRequest;
import com.dariom.wds.api.v1.dto.GuessResponse;
import com.dariom.wds.api.v1.dto.JoinRoomRequest;
import com.dariom.wds.api.v1.dto.RoomDto;
import com.dariom.wds.api.v1.dto.SubmitGuessRequest;
import com.dariom.wds.api.v1.error.ErrorResponse;
import com.dariom.wds.api.v1.mapper.RoomMapper;
import com.dariom.wds.domain.Language;
import com.dariom.wds.service.GameService;
import com.dariom.wds.service.room.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/rooms")
@Tag(name = "Rooms", description = "Room management and gameplay actions")
public class RoomController {
  // todo security
  // todo trace id

  private final RoomService roomService;
  private final GameService gameService;
  private final RoomMapper roomMapper;

  @Operation(summary = "Create room", description = "Creates a new room and joins the creator as the first player.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Room created", content = @Content(schema = @Schema(implementation = RoomDto.class))),
      @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @PostMapping
  public ResponseEntity<RoomDto> createRoom(@Valid @RequestBody CreateRoomRequest request) {
    log.info("Create room: {}", request);
    var language = Language.valueOf(request.language().trim().toUpperCase());
    var room = roomService.createRoom(language, request.playerId());
    var roomDto = roomMapper.toDto(room);
    var roomUri = getRoomUri(roomDto.id());

    return ResponseEntity.created(roomUri).body(roomDto);
  }

  @Operation(summary = "Join room", description = "Joins an existing room as the second player.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Joined room", content = @Content(schema = @Schema(implementation = RoomDto.class))),
      @ApiResponse(responseCode = "404", description = "Room not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "409", description = "Room full", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @PostMapping("/{roomId}/join")
  public ResponseEntity<RoomDto> joinRoom(
      @Parameter(description = "Room identifier", required = true) @PathVariable String roomId,
      @Valid @RequestBody JoinRoomRequest request
  ) {
    log.info("Join room <{}>: {}", roomId, request);
    var room = roomService.joinRoom(roomId, request.playerId());
    return ResponseEntity.ok(roomMapper.toDto(room));
  }

  @Operation(summary = "Get room", description = "Returns the current state of a room.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Room returned", content = @Content(schema = @Schema(implementation = RoomDto.class))),
      @ApiResponse(responseCode = "404", description = "Room not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @GetMapping("/{roomId}")
  public ResponseEntity<RoomDto> getRoom(
      @Parameter(description = "Room identifier", required = true) @PathVariable String roomId
  ) {
    log.info("Get room <{}>", roomId);
    var room = roomService.getRoom(roomId);
    return ResponseEntity.ok(roomMapper.toDto(room));
  }

  @Operation(summary = "Submit guess", description = "Submits a guess for a player in a room and returns the updated room state.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Guess accepted", content = @Content(schema = @Schema(implementation = GuessResponse.class))),
      @ApiResponse(responseCode = "400", description = "Invalid guess", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "Room not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @PostMapping("/{roomId}/guess")
  public ResponseEntity<GuessResponse> submitGuess(
      @Parameter(description = "Room identifier", required = true) @PathVariable String roomId,
      @Valid @RequestBody SubmitGuessRequest request
  ) {
    log.info("Submit guess in room <{}>: {}", roomId, request);
    var room = gameService.handleGuess(roomId, request.playerId(), request.word());
    var response = new GuessResponse(roomMapper.toDto(room));
    return ResponseEntity.ok(response);
  }

  private static URI getRoomUri(String roomId) {
    return ServletUriComponentsBuilder.fromCurrentRequest()
        .path("/{roomId}")
        .buildAndExpand(roomId)
        .toUri();
  }

}
