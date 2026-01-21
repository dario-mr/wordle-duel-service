package com.dariom.wds.service.user;

import static com.dariom.wds.config.CacheConfig.USER_PROFILE_CACHE;
import static com.dariom.wds.util.UserUtils.normalizeFullName;

import com.dariom.wds.domain.UserProfile;
import com.dariom.wds.exception.UserNotFoundException;
import com.dariom.wds.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserProfileService {

  private final UserRepository userRepository;

  @Cacheable(cacheNames = USER_PROFILE_CACHE, key = "#appUserId")
  public UserProfile getUserProfile(String appUserId) {
    var user = userRepository.findById(appUserId)
        .orElseThrow(() -> new UserNotFoundException(appUserId));

    var displayName = normalizeFullName(user.getFullName());

    return new UserProfile(appUserId, user.getFullName(), displayName, user.getPictureUrl());
  }
}
