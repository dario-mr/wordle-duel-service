package com.dariom.wds.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import com.dariom.wds.websocket.RedisRoomEventPublisher;
import com.dariom.wds.websocket.model.EventType;
import com.dariom.wds.websocket.model.RoomEvent;
import com.dariom.wds.websocket.model.RoomEventToPublish;
import com.dariom.wds.websocket.model.RoundStartedPayload;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@SpringBootTest(webEnvironment = RANDOM_PORT)
class RedisEventRelayIT extends AbstractRedisTest {

  @Autowired
  private RedisRoomEventPublisher publisher;

  @Autowired
  private RedisConnectionFactory connectionFactory;

  @Test
  void publish_roomEvent_receivedBySubscriber() throws Exception {
    // Arrange
    var latch = new CountDownLatch(1);
    var received = new CopyOnWriteArrayList<String>();

    var container = new RedisMessageListenerContainer();
    container.setConnectionFactory(connectionFactory);
    container.addMessageListener((message, pattern) -> {
      received.add(new String(message.getBody()));
      latch.countDown();
    }, new ChannelTopic(RedisRoomEventPublisher.ROOM_EVENTS));
    container.afterPropertiesSet();
    container.start();

    var event = new RoomEvent(EventType.ROUND_STARTED, new RoundStartedPayload(1, 6));
    var roomEvent = new RoomEventToPublish("room-42", event);

    // Act
    publisher.publish(roomEvent);

    // Assert
    assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    assertThat(received).hasSize(1);
    assertThat(received.getFirst()).contains("room-42");
    assertThat(received.getFirst()).contains("ROUND_STARTED");

    container.stop();
  }
}
