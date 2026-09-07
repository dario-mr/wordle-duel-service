package com.dariom.wds.api.admin.dto;

import com.dariom.wds.domain.RoundPlayerStatus;
import com.dariom.wds.domain.RoundStatus;
import java.util.Map;

public record AdminRoundDto(
    int roundNumber,
    String solution,
    RoundStatus roundStatus,
    Map<String, RoundPlayerStatus> playerStatus
) {

}
