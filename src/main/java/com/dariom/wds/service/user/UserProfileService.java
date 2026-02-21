package com.dariom.wds.service.user;

import static com.dariom.wds.config.CacheConfig.USER_PROFILE_CACHE;
import static com.dariom.wds.persistence.repository.jpa.AppUserSpecifications.displayNameContains;
import static com.dariom.wds.persistence.repository.jpa.AppUserSpecifications.emailContains;
import static com.dariom.wds.persistence.repository.jpa.AppUserSpecifications.fullNameContains;
import static java.util.stream.Collectors.toMap;

import com.dariom.wds.domain.UserProfile;
import com.dariom.wds.exception.UserNotFoundException;
import com.dariom.wds.persistence.repository.UserRepository;
import com.dariom.wds.service.DomainMapper;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserProfileService {

  private final UserRepository userRepository;
  private final DomainMapper domainMapper;
  private final UserDetailsService userDetailsService;

  @Cacheable(cacheNames = USER_PROFILE_CACHE, key = "#appUserId")
  public UserProfile getUserProfile(String appUserId) {
    var user = userRepository.findById(appUserId)
        .orElseThrow(() -> new UserNotFoundException(appUserId));

    return domainMapper.toUserProfile(user);
  }

  public Map<String, String> getDisplayNamePerPlayer(Set<String> playerIds) {
    return playerIds.stream()
        .collect(toMap(Function.identity(), userDetailsService::getUserDisplayName));
  }

  public Page<UserProfile> getAllUserProfiles(Pageable pageable, String fullName, String email,
      String displayName) {
    var spec = Specification.allOf(
        fullNameContains(fullName),
        emailContains(email),
        displayNameContains(displayName)
    );
    return userRepository.findAll(spec, pageable)
        .map(domainMapper::toUserProfile);
  }
}
