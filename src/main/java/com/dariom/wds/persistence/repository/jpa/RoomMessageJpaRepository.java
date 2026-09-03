package com.dariom.wds.persistence.repository.jpa;

import com.dariom.wds.persistence.entity.RoomMessageEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomMessageJpaRepository extends JpaRepository<RoomMessageEntity, Long> {

  List<RoomMessageEntity> findTop3ByRoomIdOrderByCreatedAtDescIdDesc(String roomId);

  List<RoomMessageEntity> findByRoomIdOrderByCreatedAtAscIdAsc(String roomId);
}
