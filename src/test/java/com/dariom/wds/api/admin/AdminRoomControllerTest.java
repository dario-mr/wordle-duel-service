package com.dariom.wds.api.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dariom.wds.api.admin.dto.AdminRoomDto;
import com.dariom.wds.api.v1.dto.PlayerDto;
import com.dariom.wds.domain.Language;
import com.dariom.wds.domain.RoomRounds;
import com.dariom.wds.domain.RoomStatus;
import com.dariom.wds.service.room.RoomService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminRoomControllerTest {

  @Mock
  private RoomService roomService;

  @InjectMocks
  private AdminRoomController controller;

  @Test
  void getAllRooms_validRequest_returnsPagedRooms() {
    var pageable = PageRequest.of(1, 10, Sort.by("createdAt"));
    var statuses = Set.of(RoomStatus.IN_PROGRESS);
    var createdAt = LocalDate.of(2025, 6, 1);
    var lastUpdatedAt = LocalDate.of(2025, 6, 2);
    var room = new AdminRoomDto(
        "room-1",
        Language.IT,
        RoomRounds.FIVE,
        RoomStatus.IN_PROGRESS,
        List.of(new PlayerDto("player-1", 2, 7, "Player One")),
        Instant.parse("2025-06-01T10:00:00Z"),
        Instant.parse("2025-06-01T10:05:00Z")
    );
    var page = new PageImpl<>(List.of(room));
    when(roomService.listRoomsForAdmin(pageable, statuses, Language.IT, RoomRounds.FIVE,
        "room", "player-1", createdAt, lastUpdatedAt)).thenReturn(page);

    var result = controller.getAllRooms(
        pageable, statuses, Language.IT, RoomRounds.FIVE, "room", "player-1", createdAt,
        lastUpdatedAt);

    assertThat(result.getContent()).containsExactly(room);
    verify(roomService).listRoomsForAdmin(pageable, statuses, Language.IT, RoomRounds.FIVE,
        "room", "player-1", createdAt, lastUpdatedAt);
  }

  @Test
  void getAllRooms_withoutStatus_forwardsAllStatuses() {
    var pageable = PageRequest.of(0, 50);
    var page = new PageImpl<AdminRoomDto>(List.of());
    when(roomService.listRoomsForAdmin(eq(pageable), eq(null), eq(null), eq(null), eq(null),
        eq(null), eq(null), eq(null))).thenReturn(page);

    var result = controller.getAllRooms(pageable, null, null, null, null, null, null, null);

    assertThat(result.getContent()).isEmpty();
    verify(roomService).listRoomsForAdmin(pageable, null, null, null, null, null, null, null);
  }

  @Test
  void getAllRooms_defaultPageable_sortsByLatestActivity() throws NoSuchMethodException {
    var pageableDefault = AdminRoomController.class
        .getDeclaredMethod("getAllRooms", Pageable.class, Set.class, Language.class,
            RoomRounds.class, String.class, String.class, LocalDate.class, LocalDate.class)
        .getParameterAnnotations()[0][0];

    assertThat(pageableDefault).isInstanceOf(PageableDefault.class);
    var annotation = (PageableDefault) pageableDefault;
    assertThat(annotation.sort()).containsExactly("lastUpdatedAt");
    assertThat(annotation.direction()).isEqualTo(Sort.Direction.DESC);
  }

  @Test
  void getAllRooms_mapsUiSortProperties() {
    var pageable = PageRequest.of(0, 50, Sort.by(
        Sort.Order.asc("roomId"),
        Sort.Order.desc("rounds"),
        Sort.Order.asc("players")
    ));
    var mappedPageable = PageRequest.of(0, 50, Sort.by(
        Sort.Order.asc("id"),
        Sort.Order.desc("configuredRounds"),
        Sort.Order.asc("players")
    ));
    var page = new PageImpl<AdminRoomDto>(List.of());
    when(roomService.listRoomsForAdmin(mappedPageable, null, null, null, null, null, null, null))
        .thenReturn(page);

    var result = controller.getAllRooms(pageable, null, null, null, null, null, null, null);

    assertThat(result.getContent()).isEmpty();
    verify(roomService).listRoomsForAdmin(mappedPageable, null, null, null, null, null, null, null);
  }

  @Test
  void deleteRoom_validRequest_returnsNoContent() {
    // Act
    var response = controller.deleteRoom("room-1");

    // Assert
    assertThat(response.getStatusCode().value()).isEqualTo(204);
    verify(roomService).deleteRoomById("room-1");
  }
}
