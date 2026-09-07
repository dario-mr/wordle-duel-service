package com.dariom.wds.api.admin.dto;

import com.dariom.wds.domain.Language;
import com.dariom.wds.domain.RoomRounds;
import com.dariom.wds.domain.RoomStatus;
import java.time.Instant;
import java.util.List;

public record AdminRoomDto(
    String id,
    Language language,
    RoomRounds configuredRounds,
    RoomStatus status,
    List<AdminPlayerDto> players,
    List<AdminRoundDto> rounds,
    Instant createdAt,
    Instant lastUpdatedAt
) {

}
