package com.dariom.wds.exception;

public class UserNotFoundException extends RuntimeException {

  public UserNotFoundException(String appUserId) {
    super("User <%s> not found".formatted(appUserId));
  }
}
