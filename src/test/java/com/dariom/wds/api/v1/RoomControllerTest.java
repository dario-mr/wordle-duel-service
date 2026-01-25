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
import com.dariom.wds.api.v1.dto.ReadyRequest;
import com.dariom.wds.api.v1.dto.SubmitGuessRequest;
import com.dariom.wds.api.v1.mapper.RoomMapper;
import com.dariom.wds.domain.Language;
import com.dariom.wds.domain.Player;
import com.dariom.wds.domain.Room;
import com.dariom.wds.domain.RoomStatus;
import com.dariom.wds.service.room.RoomService;
import com.dariom.wds.service.round.RoundService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.jwt.Jwt;
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

    var domainRoom = room(WAITING_FOR_PLAYERS);
    var expectedDto = roomMapper.toDto(domainRoom, "user-1");

    when(roomService.createRoom(any(Language.class), anyString())).thenReturn(domainRoom);

    // Act
    var response = roomController.createRoom(new CreateRoomRequest(" it "), jwtWithUid("user-1"));

    // Assert
    assertThat(response.getStatusCode().value()).isEqualTo(201);
    assertThat(response.getBody()).isEqualTo(expectedDto);
    assertThat(response.getHeaders().getLocation()).isNotNull();
    assertThat(response.getHeaders().getLocation().toString()).endsWith("/api/v1/rooms/room-1");

    verify(roomService).createRoom(IT, "user-1");
  }

  @Test
  void joinRoom_validRequest_returnsOk() {
    // Arrange
    var domainRoom = room(WAITING_FOR_PLAYERS);
    var expectedDto = roomMapper.toDto(domainRoom, "user-2");

    when(roomService.joinRoom(anyString(), anyString())).thenReturn(domainRoom);

    // Act
    var response = roomController.joinRoom("room-1", jwtWithUid("user-2"));

    // Assert
    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isEqualTo(expectedDto);

    verify(roomService).joinRoom("room-1", "user-2");
  }

  @Test
  void listRooms_validRequest_returnsOkWithRooms() {
    // Arrange
    var domainRooms = List.of(room(WAITING_FOR_PLAYERS), room(IN_PROGRESS));
    var expectedDtos = domainRooms.stream().map(r -> roomMapper.toDto(r, "user-1")).toList();

    when(roomService.listRoomsForPlayer(anyString())).thenReturn(domainRooms);

    // Act
    var response = roomController.listRooms(jwtWithUid("user-1"));

    // Assert
    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isEqualTo(expectedDtos);

    verify(roomService).listRoomsForPlayer("user-1");
  }

  @Test
  void submitGuess_validRequest_returnsOkWithGuessResponse() {
    // Arrange
    var domainRoom = room(IN_PROGRESS);
    var expectedDto = roomMapper.toDto(domainRoom, "user-1");

    when(roundService.handleGuess(anyString(), anyString(), anyString())).thenReturn(domainRoom);

    // Act
    var response = roomController.submitGuess("room-1", new SubmitGuessRequest("pizza"),
        jwtWithUid("user-1"));

    // Assert
    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().room()).isEqualTo(expectedDto);

    verify(roundService).handleGuess("room-1", "user-1", "pizza");
  }

  @Test
  void ready_validRequest_returnsOk() {
    // Arrange
    var domainRoom = room(IN_PROGRESS);
    var expectedDto = roomMapper.toDto(domainRoom, "user-1");

    when(roundService.handleReady(anyString(), anyString(), any(Integer.class))).thenReturn(
        domainRoom);

    // Act
    var response = roomController.ready("room-1", new ReadyRequest(1), jwtWithUid("user-1"));

    // Assert
    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isEqualTo(expectedDto);

    verify(roundService).handleReady("room-1", "user-1", 1);
  }

  private static Jwt jwtWithUid(String uid) {
    var now = Instant.now();
    return new Jwt(
        "test-token",
        now,
        now.plusSeconds(3600),
        Map.of("alg", "none"),
        Map.of("uid", uid)
    );
  }

  private static Room room(RoomStatus roomStatus) {
    return new Room(
        "room-1",
        IT,
        roomStatus,
        List.of(new Player("p1", 0, "John")),
        null
    );
  }

}
