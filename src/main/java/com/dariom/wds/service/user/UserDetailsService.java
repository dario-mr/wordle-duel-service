package com.dariom.wds.service.user;

import static com.dariom.wds.config.CacheConfig.DISPLAY_NAME_CACHE;
import static com.dariom.wds.util.UserUtils.ANONYMOUS;

import com.dariom.wds.persistence.entity.AppUserEntity;
import com.dariom.wds.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsService {

  private final UserRepository userRepository;

  @Cacheable(cacheNames = DISPLAY_NAME_CACHE, key = "#appUserId")
  public String getUserDisplayName(String appUserId) {
    return userRepository.findById(appUserId)
        .map(AppUserEntity::getDisplayName)
        .orElse(ANONYMOUS);
  }
}
