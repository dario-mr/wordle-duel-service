package com.dariom.wds.exception;

public class RefreshTokenEmptyException extends RuntimeException {

  public RefreshTokenEmptyException() {
    super("The refresh token cookie is empty");
  }
}
