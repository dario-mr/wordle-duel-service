package com.dariom.wds.websocket.model;

import com.dariom.wds.domain.RoundPlayerStatus;

public record PlayerStatusUpdatedPayload(RoundPlayerStatus status) implements EventPayload {

}
