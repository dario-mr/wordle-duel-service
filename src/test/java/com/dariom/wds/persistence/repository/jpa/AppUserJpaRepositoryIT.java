package com.dariom.wds.persistence.repository.jpa;

import static com.dariom.wds.persistence.repository.jpa.AppUserSpecifications.playerSearch;
import static org.assertj.core.api.Assertions.assertThat;

import com.dariom.wds.persistence.entity.AppUserEntity;
import com.dariom.wds.persistence.entity.RoleEntity;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

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

  @Test
  void findAll_playerSearch_matchesProfileFieldsPartially() {
    // Arrange
    var fullNameUser = new AppUserEntity(UUID.randomUUID(), "full@example.com", "google-full",
        "Alice Smith", "pictureUrl");
    var displayNameUser = new AppUserEntity(UUID.randomUUID(), "display@example.com", "google-display",
        "Another User", "pictureUrl");
    displayNameUser.setDisplayName("Bobby");
    var emailUser = new AppUserEntity(UUID.randomUUID(), "carol@example.com", "google-email",
        "Another User", "pictureUrl");

    repository.saveAll(List.of(fullNameUser, displayNameUser, emailUser));

    // Act
    var fullNameMatches = repository.findAll(playerSearch("smith"), Pageable.unpaged());
    var displayNameMatches = repository.findAll(playerSearch("bobb"), Pageable.unpaged());
    var emailMatches = repository.findAll(playerSearch("carol@"), Pageable.unpaged());

    // Assert
    assertThat(fullNameMatches).extracting(AppUserEntity::getId)
        .containsExactly(fullNameUser.getId());
    assertThat(displayNameMatches).extracting(AppUserEntity::getId)
        .containsExactly(displayNameUser.getId());
    assertThat(emailMatches).extracting(AppUserEntity::getId)
        .containsExactly(emailUser.getId());
  }

  private static AppUserEntity userEntity(UUID userId) {
    return new AppUserEntity(userId, "user@test.com", "google-sub-1", "User Test", "pictureUrl");
  }
}
