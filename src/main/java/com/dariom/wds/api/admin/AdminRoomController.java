package com.dariom.wds.api.admin;

import com.dariom.wds.api.common.ErrorResponse;
import com.dariom.wds.service.room.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/rooms")
@Tag(name = "Admin - Rooms", description = "Administrative room management")
public class AdminRoomController {

  private final RoomService roomService;

  @Operation(summary = "Delete room", description = "Deletes a room and all related data (rounds, guesses, players, etc).")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Room deleted"),
      @ApiResponse(responseCode = "404", description = "Room not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @DeleteMapping("/{roomId}")
  public ResponseEntity<Void> deleteRoom(
      @Parameter(description = "Room identifier", required = true) @PathVariable String roomId
  ) {
    log.info("Admin delete room <{}>", roomId);
    roomService.deleteRoomById(roomId);
    return ResponseEntity.noContent().build();
  }
}
