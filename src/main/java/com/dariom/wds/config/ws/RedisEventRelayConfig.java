package com.dariom.wds.config.ws;

import static com.dariom.wds.websocket.RedisRoomEventPublisher.ROOM_EVENTS;

import com.dariom.wds.websocket.RedisRoomEventSubscriber;
import com.dariom.wds.websocket.model.EventPayload;
import com.dariom.wds.websocket.model.EventPayloadMixin;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

@Configuration
public class RedisEventRelayConfig {

  @Bean
  RedisMessageListenerContainer redisMessageListenerContainer(
      RedisConnectionFactory connectionFactory,
      RedisRoomEventSubscriber subscriber) {
    var container = new RedisMessageListenerContainer();
    container.setConnectionFactory(connectionFactory);
    container.addMessageListener(subscriber, new ChannelTopic(ROOM_EVENTS));

    return container;
  }

  @Bean
  ObjectMapper redisEventObjectMapper(Jackson2ObjectMapperBuilder builder) {
    var mapper = builder.build();
    mapper.addMixIn(EventPayload.class, EventPayloadMixin.class);
    return mapper;
  }
}
