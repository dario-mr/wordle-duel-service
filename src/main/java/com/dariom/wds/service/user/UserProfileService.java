package com.dariom.wds.service.user;

import static com.dariom.wds.config.CacheConfig.USER_PROFILE_CACHE;

import com.dariom.wds.domain.UserProfile;
import com.dariom.wds.exception.UserNotFoundException;
import com.dariom.wds.persistence.repository.UserRepository;
import com.dariom.wds.service.DomainMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserProfileService {

  private final UserRepository userRepository;
  private final DomainMapper domainMapper;

  @Cacheable(cacheNames = USER_PROFILE_CACHE, key = "#appUserId")
  public UserProfile getUserProfile(String appUserId) {
    var user = userRepository.findById(appUserId)
        .orElseThrow(() -> new UserNotFoundException(appUserId));

    return domainMapper.toUserProfile(user);
  }

  public Page<UserProfile> getAllUserProfiles(Pageable pageable) {
    return userRepository.findAll(pageable)
        .map(domainMapper::toUserProfile);
  }
}
