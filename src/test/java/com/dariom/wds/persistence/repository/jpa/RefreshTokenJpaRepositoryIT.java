package com.dariom.wds.persistence.repository.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import com.dariom.wds.persistence.entity.AppUserEntity;
import com.dariom.wds.persistence.entity.RefreshTokenEntity;
import com.dariom.wds.persistence.entity.RoleEntity;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@JpaRepositoryIT
class RefreshTokenJpaRepositoryIT {

  @Autowired
  private RefreshTokenJpaRepository repository;

  @Autowired
  private AppUserJpaRepository userRepository;

  @Autowired
  private RoleJpaRepository roleRepository;

  @Test
  void findByTokenHash_existingToken_returnsTokenWithUserAndRolesLoaded() {
    // Arrange
    var userId = UUID.randomUUID();
    var role = roleRepository.save(new RoleEntity("USER"));

    var user = new AppUserEntity(userId, "user@test.com", "google-sub-1", "User Test");
    user.addRole(role);
    userRepository.save(user);

    var tokenId = UUID.randomUUID();
    var tokenHash = "a".repeat(64);
    var now = Instant.now();

    repository.save(new RefreshTokenEntity(tokenId, user, tokenHash, now, now.plusSeconds(60)));

    // Act
    var found = repository.findByTokenHash(tokenHash).orElseThrow();

    // Assert
    assertThat(found.getId()).isEqualTo(tokenId);
    assertThat(found.getTokenHash()).isEqualTo(tokenHash);

    var foundUser = found.getUser();
    assertThat(foundUser.getId()).isEqualTo(userId);
    assertThat(foundUser.getRoles())
        .extracting(RoleEntity::getName)
        .containsExactlyInAnyOrder("USER");
  }

  @Test
  void deleteExpired_deletesOnlyExpiredTokens() {
    // Arrange
    var userId = UUID.randomUUID();
    var user = new AppUserEntity(userId, "user@test.com", "google-sub-1", "User Test");
    userRepository.save(user);

    var now = Instant.now();
    var expiredTokenHash = "b".repeat(64);
    var validTokenHash = "c".repeat(64);

    repository.save(
        new RefreshTokenEntity(
            UUID.randomUUID(),
            user,
            expiredTokenHash,
            now.minusSeconds(120),
            now.minusSeconds(1)));
    repository.save(
        new RefreshTokenEntity(
            UUID.randomUUID(),
            user,
            validTokenHash,
            now,
            now.plusSeconds(3600)));

    // Act
    var deleted = repository.deleteExpired(now);

    // Assert
    assertThat(deleted).isEqualTo(1);
    assertThat(repository.findByTokenHash(expiredTokenHash)).isEmpty();
    assertThat(repository.findByTokenHash(validTokenHash)).isPresent();
  }
}
