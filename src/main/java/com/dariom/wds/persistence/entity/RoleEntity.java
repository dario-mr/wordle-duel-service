package com.dariom.wds.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "role")
public class RoleEntity {

  @Id
  @Column(name = "name", nullable = false, length = 32)
  private String name;

  protected RoleEntity() {
  }

  public RoleEntity(String name) {
    this.name = name;
  }

}
