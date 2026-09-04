package com.dariom.wds.service.room;

import static com.dariom.wds.domain.RoomMessagePreset.WOW;
import static com.dariom.wds.websocket.model.EventType.ROOM_MESSAGE_SENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dariom.wds.config.lock.RoomLockProperties;
import com.dariom.wds.exception.PlayerNotInRoomException;
import com.dariom.wds.exception.RoomMessageLimitReachedException;
import com.dariom.wds.exception.RoomNotReadyException;
import com.dariom.wds.persistence.entity.RoomEntity;
import com.dariom.wds.persistence.entity.RoomMessageEntity;
import com.dariom.wds.persistence.repository.RoomRepository;
import com.dariom.wds.persistence.repository.jpa.RoomMessageJpaRepository;
import com.dariom.wds.websocket.model.RoomEventToPublish;
import com.dariom.wds.websocket.model.RoomMessagePayload;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class RoomMessageServiceTest {

  @Mock
  private RoomRepository roomRepository;
  @Mock
  private RoomMessageJpaRepository roomMessageJpaRepository;
  @Mock
  private ApplicationEventPublisher eventPublisher;

  private final RoomLockProperties lockProperties = new RoomLockProperties(Duration.ofSeconds(3));
  private RoomMessageService roomMessageService;

  @BeforeEach
  void setUp() {
    roomMessageService = new RoomMessageService(roomRepository, lockProperties,
        roomMessageJpaRepository, eventPublisher);
  }

  @Test
  void sendMessage_playerInRoom_persistsAndPublishesMessage() {
    // Arrange
    when(roomRepository.findWithPlayersByIdForUpdate("room-1", lockProperties.acquireTimeout()))
        .thenReturn(room("room-1", "player-1", "player-2"));
    when(roomMessageJpaRepository.findTop3ByRoomIdOrderByCreatedAtDescIdDesc("room-1"))
        .thenReturn(List.of());
    when(roomMessageJpaRepository.save(any(RoomMessageEntity.class))).thenAnswer(invocation -> {
      var message = invocation.getArgument(0, RoomMessageEntity.class);
      message.setId(1L);
      return message;
    });

    // Act
    var result = roomMessageService.sendMessage("room-1", "player-1", WOW);

    // Assert
    assertThat(result.id()).isEqualTo(1L);
    assertThat(result.senderPlayerId()).isEqualTo("player-1");
    assertThat(result.preset()).isEqualTo(WOW);
    assertThat(result.createdAt()).isNotNull();

    var eventCaptor = ArgumentCaptor.forClass(RoomEventToPublish.class);
    verify(eventPublisher).publishEvent(eventCaptor.capture());
    assertThat(eventCaptor.getValue().roomId()).isEqualTo("room-1");
    assertThat(eventCaptor.getValue().event().type()).isEqualTo(ROOM_MESSAGE_SENT);
    assertThat(eventCaptor.getValue().event().payload())
        .isEqualTo(new RoomMessagePayload(1L, "player-1", WOW, result.createdAt()));
  }

  @Test
  void sendMessage_waitingForOpponent_isRejected() {
    // Arrange
    when(roomRepository.findWithPlayersByIdForUpdate("room-1", lockProperties.acquireTimeout()))
        .thenReturn(room("room-1", "player-1"));

    // Act / Assert
    assertThatThrownBy(() -> roomMessageService.sendMessage("room-1", "player-1", WOW))
        .isInstanceOf(RoomNotReadyException.class);

    verifyNoInteractions(roomMessageJpaRepository, eventPublisher);
  }

  @Test
  void sendMessage_afterThreeConsecutiveMessages_isRejected() {
    // Arrange
    when(roomRepository.findWithPlayersByIdForUpdate("room-1", lockProperties.acquireTimeout()))
        .thenReturn(room("room-1", "player-1", "player-2"));
    when(roomMessageJpaRepository.findTop3ByRoomIdOrderByCreatedAtDescIdDesc("room-1"))
        .thenReturn(List.of(message("player-1"), message("player-1"), message("player-1")));

    // Act / Assert
    assertThatThrownBy(() -> roomMessageService.sendMessage("room-1", "player-1", WOW))
        .isInstanceOf(RoomMessageLimitReachedException.class);

    verifyNoInteractions(eventPublisher);
  }

  @Test
  void sendMessage_afterOpponentReply_isAllowedAgain() {
    // Arrange
    when(roomRepository.findWithPlayersByIdForUpdate("room-1", lockProperties.acquireTimeout()))
        .thenReturn(room("room-1", "player-1", "player-2"));
    when(roomMessageJpaRepository.findTop3ByRoomIdOrderByCreatedAtDescIdDesc("room-1"))
        .thenReturn(List.of(message("player-2"), message("player-1"), message("player-1")));
    when(roomMessageJpaRepository.save(any(RoomMessageEntity.class))).thenAnswer(invocation -> {
      var message = invocation.getArgument(0, RoomMessageEntity.class);
      message.setId(2L);
      return message;
    });

    // Act
    var result = roomMessageService.sendMessage("room-1", "player-1", WOW);

    // Assert
    assertThat(result.senderPlayerId()).isEqualTo("player-1");
  }

  @Test
  void listMessages_nonPlayer_isRejected() {
    // Arrange
    when(roomRepository.findWithPlayersById("room-1")).thenReturn(room("room-1", "player-1"));

    // Act / Assert
    assertThatThrownBy(() -> roomMessageService.listMessages("room-1", "player-2"))
        .isInstanceOf(PlayerNotInRoomException.class);
  }

  @Test
  void listMessages_countsOnlyUnreadOpponentMessages() {
    // Arrange
    var room = room("room-1", "player-1", "player-2");
    room.findRoomPlayer("player-1").orElseThrow().setLastReadMessageId(2L);
    var messages = List.of(
        message(1L, "player-2"),
        message(2L, "player-1"),
        message(3L, "player-2"),
        message(4L, "player-1")
    );
    when(roomRepository.findWithPlayersById("room-1")).thenReturn(room);
    when(roomMessageJpaRepository.findByRoomIdOrderByCreatedAtAscIdAsc("room-1"))
        .thenReturn(messages);

    // Act
    var result = roomMessageService.listMessages("room-1", "player-1");

    // Assert
    assertThat(result.messages()).extracting(message -> message.id())
        .containsExactly(1L, 2L, 3L, 4L);
    assertThat(result.unreadCount()).isEqualTo(1);
  }

  @Test
  void markMessagesRead_advancesOnlyRequestingPlayersCursor() {
    // Arrange
    var room = room("room-1", "player-1", "player-2");
    var messages = List.of(message(1L, "player-2"), message(2L, "player-1"),
        message(3L, "player-2"));
    when(roomRepository.findWithPlayersByIdForUpdate("room-1", lockProperties.acquireTimeout()))
        .thenReturn(room);
    when(roomMessageJpaRepository.findByRoomIdOrderByCreatedAtAscIdAsc("room-1"))
        .thenReturn(messages);

    // Act
    var result = roomMessageService.markMessagesRead("room-1", "player-1");

    // Assert
    assertThat(room.findRoomPlayer("player-1").orElseThrow().getLastReadMessageId()).isEqualTo(3L);
    assertThat(room.findRoomPlayer("player-2").orElseThrow().getLastReadMessageId()).isZero();
    assertThat(result.unreadCount()).isZero();
  }

  private static RoomEntity room(String roomId, String... playerIds) {
    var room = new RoomEntity();
    room.setId(roomId);
    for (var playerId : playerIds) {
      room.addPlayer(playerId);
    }
    return room;
  }

  private static RoomMessageEntity message(String playerId) {
    var message = new RoomMessageEntity();
    message.setSenderPlayerId(playerId);
    return message;
  }

  private static RoomMessageEntity message(long id, String playerId) {
    var message = message(playerId);
    message.setId(id);
    return message;
  }
}
