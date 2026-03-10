package com.dariom.wds.websocket.model;

import static com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = NAME, property = "@payloadType")
@JsonSubTypes({
    @Type(value = PlayerJoinedPayload.class, name = "PLAYER_JOINED"),
    @Type(value = PlayerReadyPayload.class, name = "PLAYER_READY"),
    @Type(value = PlayerStatusUpdatedPayload.class, name = "PLAYER_STATUS_UPDATED"),
    @Type(value = RoundStartedPayload.class, name = "ROUND_STARTED"),
    @Type(value = RoundFinishedPayload.class, name = "ROUND_FINISHED"),
    @Type(value = ScoresUpdatedPayload.class, name = "SCORES_UPDATED"),
})
public interface EventPayloadMixin {

}
