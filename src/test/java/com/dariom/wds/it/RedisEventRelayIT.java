package com.dariom.wds.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS;

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
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@DirtiesContext(classMode = AFTER_CLASS)
class RedisEventRelayIT {

  @Container
  static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
      .withExposedPorts(6379)
      .waitingFor(Wait.forListeningPort());

  @DynamicPropertySource
  static void registerRedisProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.session.store-type", () -> "redis");
    registry.add("spring.data.redis.host", redis::getHost);
    registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    registry.add("management.health.redis.enabled", () -> "true");
  }

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
    container.addMessageListener(new MessageListener() {
      @Override
      public void onMessage(Message message, byte[] pattern) {
        received.add(new String(message.getBody()));
        latch.countDown();
      }
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
    assertThat(received.get(0)).contains("room-42");
    assertThat(received.get(0)).contains("ROUND_STARTED");

    container.stop();
  }
}
