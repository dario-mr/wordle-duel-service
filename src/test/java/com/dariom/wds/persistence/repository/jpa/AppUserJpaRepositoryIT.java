package com.dariom.wds.persistence.repository.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import com.dariom.wds.persistence.entity.AppUserEntity;
import com.dariom.wds.persistence.entity.RoleEntity;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@JpaRepositoryIT
class AppUserJpaRepositoryIT {

  @Autowired
  private AppUserJpaRepository repository;

  @Autowired
  private RoleJpaRepository roleRepository;

  @Test
  void findByEmail_existingUser_returnsUserWithRoles() {
    // Arrange
    var userId = UUID.randomUUID();
    var role = roleRepository.save(new RoleEntity("USER"));
    var user = userEntity(userId);
    user.addRole(role);

    repository.save(user);

    // Act
    var found = repository.findByEmail("user@test.com").orElseThrow();

    // Assert
    assertThat(found.getId()).isEqualTo(userId);
    assertThat(found.getEmail()).isEqualTo("user@test.com");
    assertThat(found.getGoogleSub()).isEqualTo("google-sub-1");
    assertThat(found.getFullName()).isEqualTo("User Test");
    assertThat(found.getRoles())
        .extracting(RoleEntity::getName)
        .containsExactlyInAnyOrder("USER");
  }

  @Test
  void findByEmail_unknownEmail_returnsEmpty() {
    // Act
    var found = repository.findByEmail("missing@test.com");

    // Assert
    assertThat(found).isEmpty();
  }

  @Test
  void findByGoogleSub_existingUser_returnsUserWithRoles() {
    // Arrange
    var userId = UUID.randomUUID();
    var role = roleRepository.save(new RoleEntity("USER"));
    var user = userEntity(userId);
    user.addRole(role);

    repository.save(user);

    // Act
    var found = repository.findByGoogleSub("google-sub-1").orElseThrow();

    // Assert
    assertThat(found.getId()).isEqualTo(userId);
    assertThat(found.getEmail()).isEqualTo("user@test.com");
    assertThat(found.getGoogleSub()).isEqualTo("google-sub-1");
    assertThat(found.getFullName()).isEqualTo("User Test");
    assertThat(found.getRoles())
        .extracting(RoleEntity::getName)
        .containsExactlyInAnyOrder("USER");
  }

  @Test
  void findByGoogleSub_unknownGoogleSub_returnsEmpty() {
    // Act
    var found = repository.findByGoogleSub("google-sub-missing");

    // Assert
    assertThat(found).isEmpty();
  }

  private static AppUserEntity userEntity(UUID userId) {
    return new AppUserEntity(userId, "user@test.com", "google-sub-1", "User Test", "pictureUrl");
  }
}
