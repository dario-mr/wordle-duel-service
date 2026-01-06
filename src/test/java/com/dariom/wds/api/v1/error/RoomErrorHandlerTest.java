package com.dariom.wds.api.v1.error;

import static com.dariom.wds.api.v1.error.ErrorCode.DICTIONARY_EMPTY;
import static com.dariom.wds.api.v1.error.ErrorCode.GENERIC_BAD_REQUEST;
import static com.dariom.wds.api.v1.error.ErrorCode.INVALID_LANGUAGE;
import static com.dariom.wds.api.v1.error.ErrorCode.INVALID_PLAYER_ID;
import static com.dariom.wds.api.v1.error.ErrorCode.INVALID_WORD;
import static com.dariom.wds.api.v1.error.ErrorCode.PLAYER_NOT_IN_ROOM;
import static com.dariom.wds.api.v1.error.ErrorCode.ROOM_NOT_FOUND;
import static com.dariom.wds.api.v1.error.ErrorCode.UNKNOWN_ERROR;
import static org.assertj.core.api.Assertions.assertThat;

import com.dariom.wds.api.v1.dto.CreateRoomRequest;
import com.dariom.wds.domain.Language;
import com.dariom.wds.exception.DictionaryEmptyException;
import com.dariom.wds.exception.InvalidGuessException;
import com.dariom.wds.exception.PlayerNotInRoomException;
import com.dariom.wds.exception.RoomNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

class RoomErrorHandlerTest {

  private final RoomErrorHandler handler = new RoomErrorHandler();

  @Test
  void handleArgumentNotValid_fieldIsPlayerId_returnsInvalidPlayerId() throws Exception {
    // Arrange
    var bindingResult = new BeanPropertyBindingResult(new Object(), "request");
    bindingResult.addError(new FieldError("request", "playerId", "", false, null, null,
        "playerId is required"));

    var ex = new MethodArgumentNotValidException(dummyParameter(), bindingResult);
    var request = httpRequest("POST", "/api/v1/rooms");

    // Act
    var response = handler.handleArgumentNotValid(ex, request);

    // Assert
    assertThat(response.getStatusCode().value()).isEqualTo(400);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().code()).isEqualTo(INVALID_PLAYER_ID);
    assertThat(response.getBody().message()).isEqualTo("playerId is required");
  }

  @Test
  void handleArgumentNotValid_fieldIsLanguage_returnsInvalidLanguage() throws Exception {
    // Arrange
    var bindingResult = new BeanPropertyBindingResult(new Object(), "request");
    bindingResult.addError(new FieldError("request", "language", "", false, null, null,
        "language is invalid"));

    var ex = new MethodArgumentNotValidException(dummyParameter(), bindingResult);

    // Act
    var response = handler.handleArgumentNotValid(ex, httpRequest("POST", "/api/v1/rooms"));

    // Assert
    assertThat(response.getStatusCode().value()).isEqualTo(400);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().code()).isEqualTo(INVALID_LANGUAGE);
    assertThat(response.getBody().message()).isEqualTo("language is invalid");
  }

  @Test
  void handleArgumentNotValid_noFieldError_returnsGenericBadRequest() throws Exception {
    // Arrange
    var bindingResult = new BeanPropertyBindingResult(new Object(), "request");
    var ex = new MethodArgumentNotValidException(dummyParameter(), bindingResult);

    // Act
    var response = handler.handleArgumentNotValid(ex, httpRequest("POST", "/api/v1/rooms"));

    // Assert
    assertThat(response.getStatusCode().value()).isEqualTo(400);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().code()).isEqualTo(GENERIC_BAD_REQUEST);
    assertThat(response.getBody().message()).isEqualTo("Invalid request");
  }

  @Test
  void handleArgumentNotValid_fieldIsWord_returnsInvalidWord() throws Exception {
    // Arrange
    var bindingResult = new BeanPropertyBindingResult(new Object(), "request");
    bindingResult.addError(new FieldError("request", "word", "", false, null, null,
        "word is invalid"));

    var ex = new MethodArgumentNotValidException(dummyParameter(), bindingResult);

    // Act
    var response = handler.handleArgumentNotValid(ex, httpRequest("POST", "/api/v1/rooms"));

    // Assert
    assertThat(response.getStatusCode().value()).isEqualTo(400);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().code()).isEqualTo(INVALID_WORD);
    assertThat(response.getBody().message()).isEqualTo("word is invalid");
  }

  @Test
  void handleArgumentNotValid_unknownFieldAndBlankMessage_returnsDefaultMessageAndGenericCode()
      throws Exception {
    // Arrange
    var bindingResult = new BeanPropertyBindingResult(new Object(), "request");
    bindingResult.addError(new FieldError("request", "somethingElse", "", false, null, null, "  "));

    var ex = new MethodArgumentNotValidException(dummyParameter(), bindingResult);

    // Act
    var response = handler.handleArgumentNotValid(ex, httpRequest("POST", "/api/v1/rooms"));

    // Assert
    assertThat(response.getStatusCode().value()).isEqualTo(400);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().code()).isEqualTo(GENERIC_BAD_REQUEST);
    assertThat(response.getBody().message()).isEqualTo("Invalid request");
  }

  @Test
  void handleInvalidGuess_anyErrorCode_returnsProvidedCode() {
    // Arrange
    var ex = new InvalidGuessException(INVALID_WORD, "bad word");

    // Act
    var response = handler.handleInvalidGuess(ex);

    // Assert
    assertThat(response.getStatusCode().value()).isEqualTo(400);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().code()).isEqualTo(INVALID_WORD);
    assertThat(response.getBody().message()).isEqualTo("bad word");
  }

  @Test
  void handleRoomNotFound_exception_returnsNotFound() {
    // Arrange
    var ex = new RoomNotFoundException("room-1");

    // Act
    var response = handler.handleRoomNotFound(ex);

    // Assert
    assertThat(response.getStatusCode().value()).isEqualTo(404);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().code()).isEqualTo(ROOM_NOT_FOUND);
    assertThat(response.getBody().message()).isEqualTo("Room <room-1> not found");
  }

  @Test
  void handlePlayerNotInRoom_exception_returnsForbidden() {
    // Arrange
    var ex = new PlayerNotInRoomException("p1", "room-1");

    // Act
    var response = handler.handlePlayerNotInRoom(ex);

    // Assert
    assertThat(response.getStatusCode().value()).isEqualTo(403);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().code()).isEqualTo(PLAYER_NOT_IN_ROOM);
    assertThat(response.getBody().message()).isEqualTo("Player <p1> is not in room <room-1>");
  }

  @Test
  void handleDictionaryEmpty_exception_returnsInternalServerError() {
    // Arrange
    var ex = new DictionaryEmptyException(Language.IT);

    // Act
    var response = handler.handleDictionaryEmpty(ex);

    // Assert
    assertThat(response.getStatusCode().value()).isEqualTo(500);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().code()).isEqualTo(DICTIONARY_EMPTY);
  }

  @Test
  void handleUnexpected_exception_returnsUnknownError() {
    // Arrange
    var ex = new RuntimeException("boom");
    var request = httpRequest("GET", "/api/v1/rooms/room-1");

    // Act
    var response = handler.handleUnexpected(ex, request);

    // Assert
    assertThat(response.getStatusCode().value()).isEqualTo(500);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().code()).isEqualTo(UNKNOWN_ERROR);
    assertThat(response.getBody().message()).isEqualTo("Unexpected error");
  }

  @SuppressWarnings("unused")
  private void dummy(CreateRoomRequest request) {
  }

  private MethodParameter dummyParameter() throws NoSuchMethodException {
    Method method = RoomErrorHandlerTest.class.getDeclaredMethod("dummy", CreateRoomRequest.class);
    return new MethodParameter(method, 0);
  }

  private static HttpServletRequest httpRequest(String method, String uri) {
    var request = new MockHttpServletRequest(method, uri);
    request.setServerName("localhost");
    request.setServerPort(8080);
    return request;
  }
}
