package com.dariom.wds.config.ws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.dariom.wds.config.security.AuthenticatedUser;
import com.dariom.wds.config.security.AuthenticatedUserResolver;
import com.dariom.wds.persistence.entity.RoomEntity;
import com.dariom.wds.persistence.repository.RoomRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@ExtendWith(MockitoExtension.class)
class RoomSubscriptionInterceptorTest {

  @Mock
  private AuthenticatedUserResolver authenticatedUserResolver;
  @Mock
  private RoomRepository roomRepository;
  @Mock
  private Authentication authentication;
  @Mock
  private OidcUser oidcUser;

  @Test
  void preSend_playerSubscribesToOwnRoom_allowsMessage() {
    // Arrange
    var interceptor = new RoomSubscriptionInterceptor(authenticatedUserResolver, roomRepository);
    var room = new RoomEntity();
    room.setId("room-1");
    room.addPlayer("player-1");
    when(authentication.getPrincipal()).thenReturn(oidcUser);
    when(authenticatedUserResolver.from(oidcUser))
        .thenReturn(new AuthenticatedUser("player-1", "", java.util.Set.of()));
    when(roomRepository.findWithPlayersById("room-1")).thenReturn(room);
    var message = subscription("/topic/rooms/room-1");

    // Act
    var result = interceptor.preSend(message, null);

    // Assert
    assertThat(result).isSameAs(message);
  }

  @Test
  void preSend_nonPlayerSubscribesToRoom_rejectsMessage() {
    // Arrange
    var interceptor = new RoomSubscriptionInterceptor(authenticatedUserResolver, roomRepository);
    var room = new RoomEntity();
    room.setId("room-1");
    room.addPlayer("player-1");
    when(authentication.getPrincipal()).thenReturn(oidcUser);
    when(authenticatedUserResolver.from(oidcUser))
        .thenReturn(new AuthenticatedUser("player-2", "", java.util.Set.of()));
    when(roomRepository.findWithPlayersById("room-1")).thenReturn(room);

    // Act / Assert
    assertThatThrownBy(() -> interceptor.preSend(subscription("/topic/rooms/room-1"), null))
        .isInstanceOf(com.dariom.wds.exception.PlayerNotInRoomException.class);
  }

  @Test
  void preSend_roomSubscriptionWithoutOidcPrincipal_rejectsMessage() {
    // Arrange
    var interceptor = new RoomSubscriptionInterceptor(authenticatedUserResolver, roomRepository);
    var accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    accessor.setDestination("/topic/rooms/room-1");
    var message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

    // Act / Assert
    assertThatThrownBy(() -> interceptor.preSend(message, null))
        .isInstanceOf(AccessDeniedException.class);
  }

  private Message<byte[]> subscription(String destination) {
    var accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    accessor.setDestination(destination);
    accessor.setUser(authentication);
    return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
  }
}
