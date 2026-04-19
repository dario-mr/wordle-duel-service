package com.dariom.wds.config.security;

import java.util.Set;

public record AuthenticatedUser(String userId, String email, Set<String> roles) {

}
