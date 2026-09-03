package com.dariom.wds.config.ws;

import com.dariom.wds.config.security.AuthenticatedUserResolver;
import com.dariom.wds.exception.PlayerNotInRoomException;
import com.dariom.wds.persistence.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

/**
 * Restricts {@code /topic/rooms/{roomId}} subscriptions to players in that room.
 *
 * <p>Before the broker accepts a STOMP {@code SUBSCRIBE} message, this interceptor resolves the
 * authenticated OIDC user and verifies their player ID belongs to the requested room. This keeps
 * room events and chat messages private even if a user manually targets another room's topic.
 */
@Component
@RequiredArgsConstructor
public class RoomSubscriptionInterceptor implements ChannelInterceptor {

  private static final String ROOM_TOPIC_PREFIX = "/topic/rooms/";

  private final AuthenticatedUserResolver authenticatedUserResolver;
  private final RoomRepository roomRepository;

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    var accessor = StompHeaderAccessor.wrap(message);
    if (accessor.getCommand() != StompCommand.SUBSCRIBE) {
      return message;
    }

    var roomId = roomId(accessor.getDestination());
    if (roomId == null) {
      return message;
    }

    var principal = accessor.getUser();
    if (!(principal instanceof Authentication authentication)
        || !(authentication.getPrincipal() instanceof OidcUser oidcUser)) {
      throw new AccessDeniedException("Room topic requires an authenticated OIDC user");
    }

    var playerId = authenticatedUserResolver.from(oidcUser).userId();
    var room = roomRepository.findWithPlayersById(roomId);
    if (!room.getPlayerIds().contains(playerId)) {
      throw new PlayerNotInRoomException(playerId, roomId);
    }
    return message;
  }

  private static String roomId(String destination) {
    if (destination == null || !destination.startsWith(ROOM_TOPIC_PREFIX)) {
      return null;
    }
    var roomId = destination.substring(ROOM_TOPIC_PREFIX.length());
    return roomId.isBlank() || roomId.contains("/") ? null : roomId;
  }
}
