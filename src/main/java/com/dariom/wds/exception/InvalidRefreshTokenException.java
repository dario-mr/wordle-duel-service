package com.dariom.wds.exception;

public class InvalidRefreshTokenException extends RuntimeException {

  public InvalidRefreshTokenException() {
    super("The refresh token is invalid");
  }
}
