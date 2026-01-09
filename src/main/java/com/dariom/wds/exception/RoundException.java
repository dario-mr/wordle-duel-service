package com.dariom.wds.exception;

import com.dariom.wds.api.v1.error.ErrorCode;
import lombok.Getter;

@Getter
public class RoundException extends RuntimeException {

  private final ErrorCode code;

  public RoundException(ErrorCode code, String message) {
    super(message);
    this.code = code;
  }

}
