package com.dariom.wds.api.v1.error;

import static com.dariom.wds.api.v1.error.ErrorCode.GENERIC_BAD_REQUEST;
import static com.dariom.wds.api.v1.error.ErrorCode.INVALID_LANGUAGE;
import static com.dariom.wds.api.v1.error.ErrorCode.INVALID_PLAYER_ID;
import static com.dariom.wds.api.v1.error.ErrorCode.INVALID_WORD;
import static com.dariom.wds.api.v1.error.ErrorCode.PLAYER_NOT_IN_ROOM;
import static com.dariom.wds.api.v1.error.ErrorCode.ROOM_FULL;
import static com.dariom.wds.api.v1.error.ErrorCode.ROOM_NOT_FOUND;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import com.dariom.wds.exception.InvalidGuessException;
import com.dariom.wds.exception.PlayerNotInRoomException;
import com.dariom.wds.exception.RoomFullException;
import com.dariom.wds.exception.RoomNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class RoomErrorHandler {

  @ExceptionHandler(RoomNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleRoomNotFound(RoomNotFoundException ex) {
    log.error("Room not found", ex);
    return ResponseEntity.status(NOT_FOUND)
        .body(new ErrorResponse(ROOM_NOT_FOUND, ex.getMessage()));
  }

  @ExceptionHandler(RoomFullException.class)
  public ResponseEntity<ErrorResponse> handleRoomFull(RoomFullException ex) {
    log.error("Room full", ex);
    return ResponseEntity.status(CONFLICT)
        .body(new ErrorResponse(ROOM_FULL, ex.getMessage()));
  }

  @ExceptionHandler(PlayerNotInRoomException.class)
  public ResponseEntity<ErrorResponse> handlePlayerNotInRoom(PlayerNotInRoomException ex) {
    log.error("Player not in room", ex);
    return ResponseEntity.status(FORBIDDEN)
        .body(new ErrorResponse(PLAYER_NOT_IN_ROOM, ex.getMessage()));
  }

  @ExceptionHandler(InvalidGuessException.class)
  public ResponseEntity<ErrorResponse> handleInvalidGuess(InvalidGuessException ex) {
    log.error("Invalid guess", ex);
    return ResponseEntity.status(BAD_REQUEST)
        .body(new ErrorResponse(ex.getCode(), ex.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleArgumentNotValid(MethodArgumentNotValidException ex) {
    log.error("Invalid argument", ex);

    var fieldError = ex.getBindingResult().getFieldError();
    if (fieldError == null) {
      return ResponseEntity.status(BAD_REQUEST)
          .body(new ErrorResponse(GENERIC_BAD_REQUEST, "Invalid request"));
    }

    var errorCode = switch (fieldError.getField()) {
      case "playerId" -> INVALID_PLAYER_ID;
      case "word" -> INVALID_WORD;
      case "language" -> INVALID_LANGUAGE;
      default -> GENERIC_BAD_REQUEST;
    };
    var message = defaultIfBlank(fieldError.getDefaultMessage(), "Invalid request");

    return ResponseEntity.status(BAD_REQUEST)
        .body(new ErrorResponse(errorCode, message));
  }

}
