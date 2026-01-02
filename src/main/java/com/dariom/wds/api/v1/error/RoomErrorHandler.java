package com.dariom.wds.api.v1.error;

import static com.dariom.wds.api.v1.error.ErrorCode.GENERIC_BAD_REQUEST;
import static com.dariom.wds.api.v1.error.ErrorCode.INVALID_LANGUAGE;
import static com.dariom.wds.api.v1.error.ErrorCode.INVALID_PLAYER_ID;
import static com.dariom.wds.api.v1.error.ErrorCode.INVALID_WORD;
import static com.dariom.wds.api.v1.error.ErrorCode.PLAYER_NOT_IN_ROOM;
import static com.dariom.wds.api.v1.error.ErrorCode.ROOM_FULL;
import static com.dariom.wds.api.v1.error.ErrorCode.ROOM_NOT_FOUND;
import static com.dariom.wds.api.v1.error.ErrorCode.UNKNOWN_ERROR;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import com.dariom.wds.exception.InvalidGuessException;
import com.dariom.wds.exception.PlayerNotInRoomException;
import com.dariom.wds.exception.RoomFullException;
import com.dariom.wds.exception.RoomNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class RoomErrorHandler {

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest req) {
    log.error("Unhandled error: {} {}", req.getMethod(), req.getRequestURI(), ex);
    return ResponseEntity.status(INTERNAL_SERVER_ERROR)
        .body(new ErrorResponse(UNKNOWN_ERROR, "Unexpected error"));
  }

  @ExceptionHandler(RoomNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleRoomNotFound(RoomNotFoundException ex) {
    log.warn(ex.getMessage());
    return ResponseEntity.status(NOT_FOUND)
        .body(new ErrorResponse(ROOM_NOT_FOUND, ex.getMessage()));
  }

  @ExceptionHandler(RoomFullException.class)
  public ResponseEntity<ErrorResponse> handleRoomFull(RoomFullException ex) {
    log.warn(ex.getMessage());
    return ResponseEntity.status(CONFLICT)
        .body(new ErrorResponse(ROOM_FULL, ex.getMessage()));
  }

  @ExceptionHandler(PlayerNotInRoomException.class)
  public ResponseEntity<ErrorResponse> handlePlayerNotInRoom(PlayerNotInRoomException ex) {
    log.warn(ex.getMessage());
    return ResponseEntity.status(FORBIDDEN)
        .body(new ErrorResponse(PLAYER_NOT_IN_ROOM, ex.getMessage()));
  }

  @ExceptionHandler(InvalidGuessException.class)
  public ResponseEntity<ErrorResponse> handleInvalidGuess(InvalidGuessException ex) {
    log.warn("Invalid guess: code={}, message={}", ex.getCode(), ex.getMessage());
    return ResponseEntity.status(BAD_REQUEST)
        .body(new ErrorResponse(ex.getCode(), ex.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleArgumentNotValid(MethodArgumentNotValidException ex,
      HttpServletRequest request) {
    var fieldError = ex.getBindingResult().getFieldError();
    if (fieldError == null) {
      log.warn("Request validation failed: no fieldError");
      return ResponseEntity.status(BAD_REQUEST)
          .body(new ErrorResponse(GENERIC_BAD_REQUEST, "Invalid request"));
    }

    var message = defaultIfBlank(fieldError.getDefaultMessage(), "Invalid request");
    log.warn("Request validation failed: endpoint={} {}, field={}, rejectedValue={}, message={}",
        request.getMethod(), request.getRequestURI(),
        fieldError.getField(), fieldError.getRejectedValue(), message);

    var errorCode = switch (fieldError.getField()) {
      case "playerId" -> INVALID_PLAYER_ID;
      case "word" -> INVALID_WORD;
      case "language" -> INVALID_LANGUAGE;
      default -> GENERIC_BAD_REQUEST;
    };

    return ResponseEntity.status(BAD_REQUEST)
        .body(new ErrorResponse(errorCode, message));
  }

}
