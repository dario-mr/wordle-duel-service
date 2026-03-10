package com.dariom.wds.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import com.dariom.wds.domain.RoundPlayerStatus;
import com.dariom.wds.websocket.model.EventPayload;
import com.dariom.wds.websocket.model.EventPayloadMixin;
import com.dariom.wds.websocket.model.EventType;
import com.dariom.wds.websocket.model.PlayerJoinedPayload;
import com.dariom.wds.websocket.model.PlayerReadyPayload;
import com.dariom.wds.websocket.model.PlayerStatusUpdatedPayload;
import com.dariom.wds.websocket.model.RoomEvent;
import com.dariom.wds.websocket.model.RoomEventToPublish;
import com.dariom.wds.websocket.model.RoundFinishedPayload;
import com.dariom.wds.websocket.model.RoundStartedPayload;
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
        roomEvent(EventType.ROUND_STARTED, new RoundStartedPayload(1, 6)),
        roomEvent(EventType.ROUND_FINISHED, new RoundFinishedPayload(1)),
        roomEvent(EventType.ROOM_CREATED, new PlayerJoinedPayload("player-1", List.of("player-1"))),
        roomEvent(EventType.SCORES_UPDATED, new ScoresUpdatedPayload(Map.of("p1", 10, "p2", 5))),
        roomEvent(EventType.PLAYER_STATUS_UPDATED,
            new PlayerStatusUpdatedPayload(RoundPlayerStatus.WON)),
        roomEvent(EventType.ROOM_CREATED, new PlayerReadyPayload("player-1"))
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
