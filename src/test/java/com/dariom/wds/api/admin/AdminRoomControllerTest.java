package com.dariom.wds.api.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.dariom.wds.service.room.RoomService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
  void deleteRoom_validRequest_returnsNoContent() {
    // Act
    var response = controller.deleteRoom("room-1");

    // Assert
    assertThat(response.getStatusCode().value()).isEqualTo(204);
    verify(roomService).deleteRoomById("room-1");
  }
}
