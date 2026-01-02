package com.dariom.wds.domain;

import com.dariom.wds.persistence.entity.RoomEntity;
import com.dariom.wds.persistence.entity.RoundEntity;

public record Room(
    RoomEntity room,
    RoundEntity currentRound
) {

}
