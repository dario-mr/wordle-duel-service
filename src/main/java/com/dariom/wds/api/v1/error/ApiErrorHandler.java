package com.dariom.wds.api.v1.error;

import static com.dariom.wds.api.common.ErrorCode.DICTIONARY_EMPTY;
import static com.dariom.wds.api.common.ErrorCode.GENERIC_BAD_REQUEST;
import static com.dariom.wds.api.common.ErrorCode.INVALID_LANGUAGE;
import static com.dariom.wds.api.common.ErrorCode.INVALID_ROUND_NUMBER;
import static com.dariom.wds.api.common.ErrorCode.INVALID_WORD;
import static com.dariom.wds.api.common.ErrorCode.PLAYER_NOT_IN_ROOM;
import static com.dariom.wds.api.common.ErrorCode.ROOM_ACCESS_DENIED;
import static com.dariom.wds.api.common.ErrorCode.ROOM_BUSY;
import static com.dariom.wds.api.common.ErrorCode.ROOM_CLOSED;
import static com.dariom.wds.api.common.ErrorCode.ROOM_FULL;
import static com.dariom.wds.api.common.ErrorCode.ROOM_NOT_FOUND;
import static com.dariom.wds.api.common.ErrorCode.ROOM_NOT_READY;
import static com.dariom.wds.api.common.ErrorCode.USER_NOT_FOUND;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import com.dariom.wds.api.common.ErrorResponse;
import com.dariom.wds.exception.DictionaryEmptyException;
import com.dariom.wds.exception.InvalidGuessException;
import com.dariom.wds.exception.PlayerNotInRoomException;
import com.dariom.wds.exception.RoomAccessDeniedException;
import com.dariom.wds.exception.RoomClosedException;
import com.dariom.wds.exception.RoomFullException;
import com.dariom.wds.exception.RoomLockedException;
import com.dariom.wds.exception.RoomNotFoundException;
import com.dariom.wds.exception.RoomNotReadyException;
import com.dariom.wds.exception.RoundException;
import com.dariom.wds.exception.UserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@Order(1)
@RestControllerAdvice
public class ApiErrorHandler {

  @ExceptionHandler(RoomAccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleRoomAccessDenied(RoomAccessDeniedException ex) {
    log.warn(ex.getMessage());
    return ResponseEntity.status(FORBIDDEN)
        .body(new ErrorResponse(ROOM_ACCESS_DENIED, ex.getMessage()));
  }

  @ExceptionHandler(UserNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
    log.warn(ex.getMessage());
    return ResponseEntity.status(NOT_FOUND)
        .body(new ErrorResponse(USER_NOT_FOUND, ex.getMessage()));
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

  @ExceptionHandler(RoomClosedException.class)
  public ResponseEntity<ErrorResponse> handleRoomClosed(RoomClosedException ex) {
    log.warn(ex.getMessage());
    return ResponseEntity.status(CONFLICT)
        .body(new ErrorResponse(ROOM_CLOSED, ex.getMessage()));
  }

  @ExceptionHandler(RoomNotReadyException.class)
  public ResponseEntity<ErrorResponse> handleRoomNotReady(RoomNotReadyException ex) {
    log.warn(ex.getMessage());
    return ResponseEntity.status(CONFLICT)
        .body(new ErrorResponse(ROOM_NOT_READY, ex.getMessage()));
  }

  @ExceptionHandler(RoomLockedException.class)
  public ResponseEntity<ErrorResponse> handleRoomLocked(RoomLockedException ex) {
    log.warn(ex.getMessage());
    return ResponseEntity.status(CONFLICT)
        .body(new ErrorResponse(ROOM_BUSY, ex.getMessage()));
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

  @ExceptionHandler(RoundException.class)
  public ResponseEntity<ErrorResponse> handleRoundException(RoundException ex) {
    log.warn("Invalid round: code={}, message={}", ex.getCode(), ex.getMessage());
    return ResponseEntity.status(CONFLICT)
        .body(new ErrorResponse(ex.getCode(), ex.getMessage()));
  }

  @ExceptionHandler(DictionaryEmptyException.class)
  public ResponseEntity<ErrorResponse> handleDictionaryEmpty(DictionaryEmptyException ex) {
    log.error(ex.getMessage());
    return ResponseEntity.status(INTERNAL_SERVER_ERROR)
        .body(new ErrorResponse(DICTIONARY_EMPTY, ex.getMessage()));
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
      case "word" -> INVALID_WORD;
      case "language" -> INVALID_LANGUAGE;
      case "roundNumber" -> INVALID_ROUND_NUMBER;
      default -> GENERIC_BAD_REQUEST;
    };

    return ResponseEntity.status(BAD_REQUEST)
        .body(new ErrorResponse(errorCode, message));
  }

}
