package com.dariom.wds.persistence.repository;

import static com.dariom.wds.domain.Role.USER;
import static com.dariom.wds.util.UserUtils.normalizeFullName;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.dariom.wds.domain.Role;
import com.dariom.wds.persistence.entity.AppUserEntity;
import com.dariom.wds.persistence.repository.jpa.AppUserJpaRepository;
import com.dariom.wds.persistence.repository.jpa.RoleJpaRepository;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserRepository {

  private final AppUserJpaRepository appUserJpaRepository;
  private final RoleJpaRepository roleJpaRepository;

  @Transactional
  public AppUserEntity findOrCreate(String googleSub, String email, String fullName,
      String pictureUrl) {
    var user = appUserJpaRepository.findByGoogleSub(googleSub)
        .or(() -> appUserJpaRepository.findByEmail(email)
            .map(userEntity -> {
              userEntity.setGoogleSub(googleSub);
              return userEntity;
            }))
        .orElseGet(
            () -> new AppUserEntity(UUID.randomUUID(), email, googleSub, fullName, pictureUrl));

    applyIfNotBlank(fullName, user::setFullName);
    applyIfNotBlank(pictureUrl, user::setPictureUrl);
    user.setDisplayName(normalizeFullName(fullName));
    ensureRole(user, USER);

    return appUserJpaRepository.save(user);
  }

  @Transactional(readOnly = true)
  public AppUserEntity requireByEmail(String email) {
    return appUserJpaRepository.findByEmail(email)
        .orElseThrow(() -> new IllegalArgumentException("Unknown user: " + email));
  }

  @Transactional(readOnly = true)
  public Optional<AppUserEntity> findById(String appUserId) {
    try {
      return appUserJpaRepository.findById(UUID.fromString(appUserId));
    } catch (IllegalArgumentException ex) {
      log.error("Provided userId <{}> is not a valid UUID, returning empty user entity", appUserId);
      return Optional.empty();
    }
  }

  @Transactional(readOnly = true)
  public Page<AppUserEntity> findAll(Specification<AppUserEntity> spec, Pageable pageable) {
    return appUserJpaRepository.findAll(spec, pageable);
  }

  private void ensureRole(AppUserEntity user, Role role) {
    var roleEntity = roleJpaRepository.findById(role.getName())
        .orElseThrow(() -> new IllegalStateException("RoleEntity not found: " + role.getName()));
    user.addRole(roleEntity);
  }

  private static void applyIfNotBlank(String value, Consumer<String> setter) {
    if (isNotBlank(value)) {
      setter.accept(value);
    }
  }
}
