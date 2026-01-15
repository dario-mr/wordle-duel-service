package com.dariom.wds.persistence.repository;

import static com.dariom.wds.domain.Role.USER;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.dariom.wds.domain.Role;
import com.dariom.wds.persistence.entity.AppUserEntity;
import com.dariom.wds.persistence.repository.jpa.AppUserJpaRepository;
import com.dariom.wds.persistence.repository.jpa.RoleJpaRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserRepository {

  private final AppUserJpaRepository appUserJpaRepository;
  private final RoleJpaRepository roleJpaRepository;

  @Transactional
  public AppUserEntity findOrCreate(String googleSub, String email, String fullName) {
    var optExistingBySub = appUserJpaRepository.findByGoogleSub(googleSub);
    if (optExistingBySub.isPresent()) {
      var existingBySub = optExistingBySub.get();
      if (isNotBlank(fullName)) {
        existingBySub.setFullName(fullName);
      }

      ensureRole(existingBySub, USER);
      return appUserJpaRepository.save(existingBySub);
    }

    // If user exists by email, link googleSub
    var optExistingByEmail = appUserJpaRepository.findByEmail(email);
    if (optExistingByEmail.isPresent()) {
      var existingByEmail = optExistingByEmail.get();
      existingByEmail.setGoogleSub(googleSub);
      if (isNotBlank(fullName)) {
        existingByEmail.setFullName(fullName);
      }

      ensureRole(existingByEmail, USER);
      return appUserJpaRepository.save(existingByEmail);
    }

    var created = new AppUserEntity(UUID.randomUUID(), email, googleSub, fullName);
    ensureRole(created, USER);
    return appUserJpaRepository.save(created);
  }

  @Transactional(readOnly = true)
  public AppUserEntity requireByEmail(String email) {
    return appUserJpaRepository.findByEmail(email)
        .orElseThrow(() -> new IllegalArgumentException("Unknown user: " + email));
  }

  private void ensureRole(AppUserEntity user, Role role) {
    var roleEntity = roleJpaRepository.findById(role.getName())
        .orElseThrow(() -> new IllegalStateException("RoleEntity not found: " + role.getName()));
    user.addRole(roleEntity);
  }
}
