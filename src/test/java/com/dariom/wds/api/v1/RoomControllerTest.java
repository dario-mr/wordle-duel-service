package com.dariom.wds.api.v1;

import static com.dariom.wds.domain.Language.IT;
import static com.dariom.wds.domain.RoomStatus.IN_PROGRESS;
import static com.dariom.wds.domain.RoomStatus.WAITING_FOR_PLAYERS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dariom.wds.api.v1.dto.CreateRoomRequest;
import com.dariom.wds.api.v1.dto.JoinRoomRequest;
import com.dariom.wds.api.v1.dto.SubmitGuessRequest;
import com.dariom.wds.api.v1.mapper.RoomMapper;
import com.dariom.wds.domain.Language;
import com.dariom.wds.domain.Player;
import com.dariom.wds.domain.Room;
import com.dariom.wds.service.room.RoomService;
import com.dariom.wds.service.round.RoundService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@ExtendWith(MockitoExtension.class)
class RoomControllerTest {

  @Mock
  private RoomService roomService;

  @Mock
  private RoundService roundService;

  private final RoomMapper roomMapper = new RoomMapper();

  private RoomController roomController;

  @BeforeEach
  void setUp() {
    roomController = new RoomController(roomService, roundService, roomMapper);
  }

  @AfterEach
  void tearDown() {
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  void createRoom_validRequest_returnsCreatedWithLocation() {
    // Arrange
    var request = new MockHttpServletRequest("POST", "/api/v1/rooms");
    request.setServerName("localhost");
    request.setServerPort(8080);
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

    var domainRoom = new Room(
        "room-1",
        IT,
        WAITING_FOR_PLAYERS,
        List.of(new Player("p1", 0)),
        null
    );
    var expectedDto = roomMapper.toDto(domainRoom);

    when(roomService.createRoom(any(Language.class), anyString())).thenReturn(domainRoom);

    // Act
    var response = roomController.createRoom(new CreateRoomRequest("p1", " it "));

    // Assert
    assertThat(response.getStatusCode().value()).isEqualTo(201);
    assertThat(response.getBody()).isEqualTo(expectedDto);
    assertThat(response.getHeaders().getLocation()).isNotNull();
    assertThat(response.getHeaders().getLocation().toString()).endsWith("/api/v1/rooms/room-1");

    verify(roomService).createRoom(IT, "p1");
  }

  @Test
  void joinRoom_validRequest_returnsOk() {
    // Arrange
    var domainRoom = new Room(
        "room-1",
        IT,
        WAITING_FOR_PLAYERS,
        List.of(new Player("p1", 0)),
        null
    );
    var expectedDto = roomMapper.toDto(domainRoom);

    when(roomService.joinRoom(anyString(), anyString())).thenReturn(domainRoom);

    // Act
    var response = roomController.joinRoom("room-1", new JoinRoomRequest("p2"));

    // Assert
    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isEqualTo(expectedDto);

    verify(roomService).joinRoom("room-1", "p2");
  }

  @Test
  void submitGuess_validRequest_returnsOkWithGuessResponse() {
    // Arrange
    var domainRoom = new Room(
        "room-1",
        IT,
        IN_PROGRESS,
        List.of(new Player("p1", 0)),
        null
    );
    var expectedDto = roomMapper.toDto(domainRoom);

    when(roundService.handleGuess(anyString(), anyString(), anyString())).thenReturn(domainRoom);

    // Act
    var response = roomController.submitGuess("room-1", new SubmitGuessRequest("p1", "pizza"));

    // Assert
    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().room()).isEqualTo(expectedDto);

    verify(roundService).handleGuess("room-1", "p1", "pizza");
  }

}
