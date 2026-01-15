package com.dariom.wds.persistence.repository.jpa;

import com.dariom.wds.persistence.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleJpaRepository extends JpaRepository<RoleEntity, String> {

}
