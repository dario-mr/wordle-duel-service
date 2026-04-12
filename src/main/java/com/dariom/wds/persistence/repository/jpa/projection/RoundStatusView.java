package com.dariom.wds.persistence.repository.jpa.projection;

import com.dariom.wds.domain.RoundPlayerStatus;

public record RoundStatusView(long roundId, String playerId, RoundPlayerStatus status) {

}
