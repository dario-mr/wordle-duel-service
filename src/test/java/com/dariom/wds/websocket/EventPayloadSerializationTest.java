package com.dariom.wds.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import com.dariom.wds.websocket.model.EventPayload;
import com.dariom.wds.websocket.model.EventPayloadMixin;
import com.dariom.wds.websocket.model.EventType;
import com.dariom.wds.websocket.model.PlayerJoinedPayload;
import com.dariom.wds.websocket.model.RematchStartedPayload;
import com.dariom.wds.websocket.model.RoomEvent;
import com.dariom.wds.websocket.model.RoomEventToPublish;
import com.dariom.wds.websocket.model.ScoresUpdatedPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class EventPayloadSerializationTest {

  private final ObjectMapper objectMapper = createObjectMapper();

  private static ObjectMapper createObjectMapper() {
    var mapper = new ObjectMapper();
    mapper.addMixIn(EventPayload.class, EventPayloadMixin.class);
    return mapper;
  }

  static Stream<RoomEventToPublish> roomEvents() {
    return Stream.of(
        roomEvent(EventType.ROOM_CREATED, new PlayerJoinedPayload("player-1", List.of("player-1"))),
        roomEvent(EventType.PLAYER_JOINED,
            new PlayerJoinedPayload("player-2", List.of("player-1", "player-2"))),
        roomEvent(EventType.ROOM_CLOSED, new ScoresUpdatedPayload(Map.of("p1", 10, "p2", 5))),
        roomEvent(EventType.SCORES_UPDATED, new ScoresUpdatedPayload(Map.of("p1", 10, "p2", 5))),
        roomEvent(EventType.REMATCH_STARTED, new RematchStartedPayload("room-2"))
    );
  }

  @ParameterizedTest
  @MethodSource("roomEvents")
  void roundTrip_serializeAndDeserialize_preservesPayloadType(RoomEventToPublish original)
      throws Exception {
    // Act
    var json = objectMapper.writeValueAsString(original);
    var deserialized = objectMapper.readValue(json, RoomEventToPublish.class);

    // Assert
    assertThat(deserialized).isEqualTo(original);
    assertThat(deserialized.event().payload()).isInstanceOf(original.event().payload().getClass());
  }

  private static RoomEventToPublish roomEvent(EventType type, EventPayload payload) {
    return new RoomEventToPublish("room-1", new RoomEvent(type, payload));
  }
}
