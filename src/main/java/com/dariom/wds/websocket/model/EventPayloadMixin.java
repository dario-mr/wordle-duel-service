package com.dariom.wds.websocket.model;

import static com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = NAME, property = "@payloadType")
@JsonSubTypes({
    @Type(value = PlayerJoinedPayload.class, name = "PLAYER_JOINED"),
    @Type(value = ScoresUpdatedPayload.class, name = "SCORES_UPDATED"),
    @Type(value = RematchStartedPayload.class, name = "REMATCH_STARTED"),
})
public interface EventPayloadMixin {

}
