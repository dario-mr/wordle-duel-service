package com.dariom.wds.api.admin;

import static org.springframework.data.domain.Sort.Direction.DESC;

import com.dariom.wds.api.admin.dto.AdminRoomDto;
import com.dariom.wds.api.common.ErrorResponse;
import com.dariom.wds.domain.Language;
import com.dariom.wds.domain.RoomRounds;
import com.dariom.wds.domain.RoomStatus;
import com.dariom.wds.service.room.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/rooms")
@Tag(name = "Admin - Rooms", description = "Administrative room management")
public class AdminRoomController {

  private static final Map<String, String> SORT_PROPERTIES = Map.of(
      "roomId", "id",
      "rounds", "configuredRounds"
  );

  private final RoomService roomService;

  @Operation(summary = "List rooms", description = "Returns a paginated list of rooms.")
  @GetMapping
  public Page<AdminRoomDto> getAllRooms(
      @PageableDefault(size = 50, sort = "lastUpdatedAt", direction = DESC) Pageable pageable,
      @RequestParam(name = "status", required = false) Set<RoomStatus> statuses,
      @RequestParam(name = "language", required = false) Language language,
      @RequestParam(name = "rounds", required = false) RoomRounds rounds,
      @RequestParam(name = "roomId", required = false) String roomId,
      @RequestParam(name = "playerSearch", required = false) String playerSearch,
      @RequestParam(name = "createdAt", required = false) LocalDate createdAt,
      @RequestParam(name = "lastUpdatedAt", required = false) LocalDate lastUpdatedAt
  ) {
    log.info(
        "Admin get all rooms: statuses={}, language={}, rounds={}, roomId=<{}>, playerSearch=<{}>, createdAt={}, lastUpdatedAt={}, pageable={}",
        statuses, language, rounds, roomId, playerSearch, createdAt, lastUpdatedAt, pageable);
    return roomService.listRoomsForAdmin(mapSortProperties(pageable), statuses, language, rounds,
        roomId, playerSearch, createdAt, lastUpdatedAt);
  }

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

  private Pageable mapSortProperties(Pageable pageable) {
    var orders = pageable.getSort().stream()
        .map(order -> new Sort.Order(
            order.getDirection(),
            SORT_PROPERTIES.getOrDefault(order.getProperty(), order.getProperty())
        ))
        .toList();
    var sort = orders.isEmpty() ? Sort.unsorted() : Sort.by(orders);
    return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
  }
}
