package com.dariom.wds.service.user;

import static java.util.stream.Collectors.toMap;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserDetailsService userDetailsService;

  public Map<String, String> getDisplayNamePerPlayer(Set<String> playerIds) {
    return playerIds.stream()
        .collect(toMap(Function.identity(), userDetailsService::getUserDisplayName));
  }
}
