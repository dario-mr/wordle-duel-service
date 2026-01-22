package com.dariom.wds.api.v1;

import com.dariom.wds.api.common.ErrorResponse;
import com.dariom.wds.api.v1.dto.CreateRoomRequest;
import com.dariom.wds.api.v1.dto.GuessResponse;
import com.dariom.wds.api.v1.dto.ReadyRequest;
import com.dariom.wds.api.v1.dto.RoomDto;
import com.dariom.wds.api.v1.dto.SubmitGuessRequest;
import com.dariom.wds.api.v1.mapper.RoomMapper;
import com.dariom.wds.domain.Language;
import com.dariom.wds.service.room.RoomService;
import com.dariom.wds.service.round.RoundService;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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

  private final RoomService roomService;
  private final RoundService roundService;
  private final RoomMapper roomMapper;

  @Operation(summary = "Create room", description = "Creates a new room and joins the creator as the first player.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Room created", content = @Content(schema = @Schema(implementation = RoomDto.class))),
      @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @PostMapping
  public ResponseEntity<RoomDto> createRoom(
      @Valid @RequestBody CreateRoomRequest request,
      @AuthenticationPrincipal Jwt jwt
  ) {
    var appUserId = jwt.getClaimAsString("uid");
    log.info("Create room {} by user <{}>", request, appUserId);
    var language = Language.valueOf(request.language().trim().toUpperCase());
    var room = roomService.createRoom(language, appUserId);
    var roomDto = roomMapper.toDto(room, appUserId);
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
      @AuthenticationPrincipal Jwt jwt
  ) {
    var appUserId = jwt.getClaimAsString("uid");
    log.info("Join room <{}> by user <{}>", roomId, appUserId);
    var room = roomService.joinRoom(roomId, appUserId);
    return ResponseEntity.ok(roomMapper.toDto(room, appUserId));
  }

  @Operation(summary = "Get room", description = "Returns the current state of a room.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Room returned", content = @Content(schema = @Schema(implementation = RoomDto.class))),
      @ApiResponse(responseCode = "404", description = "Room not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @GetMapping("/{roomId}")
  public ResponseEntity<RoomDto> getRoom(
      @Parameter(description = "Room identifier", required = true) @PathVariable String roomId,
      @AuthenticationPrincipal Jwt jwt
  ) {
    var appUserId = jwt.getClaimAsString("uid");
    // TODO block inspecting room from a 3rd player who is not in it?
    log.info("Get room <{}>", roomId);
    var room = roomService.getRoom(roomId);
    return ResponseEntity.ok(roomMapper.toDto(room, appUserId));
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
      @Valid @RequestBody SubmitGuessRequest request,
      @AuthenticationPrincipal Jwt jwt
  ) {
    var appUserId = jwt.getClaimAsString("uid");
    log.info("Submit guess in room <{}> by user <{}>: {}", roomId, appUserId, request);
    var room = roundService.handleGuess(roomId, appUserId, request.word());
    var response = new GuessResponse(roomMapper.toDto(room, appUserId));
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "Ready for next round", description = "Marks a player as ready for the next round. When both players are ready, the next round starts.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Ready accepted", content = @Content(schema = @Schema(implementation = RoomDto.class))),
      @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "Room not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "409", description = "Conflict", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @PostMapping("/{roomId}/ready")
  public ResponseEntity<RoomDto> ready(
      @Parameter(description = "Room identifier", required = true) @PathVariable String roomId,
      @Valid @RequestBody ReadyRequest request,
      @AuthenticationPrincipal Jwt jwt
  ) {
    var appUserId = jwt.getClaimAsString("uid");
    log.info("Player ready in room <{}> by user <{}>: {}", roomId, appUserId, request);
    var room = roundService.handleReady(roomId, appUserId, request.roundNumber());
    return ResponseEntity.ok(roomMapper.toDto(room, appUserId));
  }

  private static URI getRoomUri(String roomId) {
    return ServletUriComponentsBuilder.fromCurrentRequest()
        .path("/{roomId}")
        .buildAndExpand(roomId)
        .toUri();
  }

}
