# Multiplayer Wordle Backend – Implementation Notes (Spring Boot)

## 1. Overview

- Stack:
    - Java 21+
    - Spring Boot 3.x
    - Spring Web (REST)
    - Spring WebSocket + STOMP (for room events, not guesses)
- Data:
    - Rooms and game state stored in **H2 in-memory DB** via Spring Data JPA.
    - Makes switching to a real DB later a property change only.
    - Words (dictionary) also stored in DB.

### Core Requirements

- Only **guest login**:
    - Frontend sends `playerId` (string) with each request.
    - No authentication; just trust the `playerId`, in this first version.
- Rooms:
    - Each room has **exactly 0–2 players**.
    - Create or join by **roomId** (string).
    - **Infinite games** per room (multiple rounds).
    - Track **scores per player** within a room.
- Wordle-like game:
    - Guess word, get letter feedback: CORRECT / PRESENT / ABSENT.
    - Language-aware; for now, **only Italian**.

---

## 2. Domain Model (Backend Entities)

Define **domain classes** (not exposed directly to the client).

### 2.1 Enums

```java
public enum Language {
  IT // extend later (EN, DE, ...)
}

public enum RoomStatus {
  WAITING_FOR_PLAYERS,
  IN_PROGRESS,
  CLOSED
}

public enum RoundPlayerStatus {
  PLAYING,
  WON,
  LOST
}

public enum LetterStatus {
  CORRECT, // right letter, right position
  PRESENT, // right letter, wrong position
  ABSENT   // not in target word
}
```

### 2.2 Core Value Objects

`playerId` and `roomId` are represented as strings.

### 2.3 Room Entity

```java
public class Room {

  private String id;
  private Language language;
  private RoomStatus status;
  private List<String> playerIds; // size <= 2, order unimportant or [p1, p2]

  // total points in this room
  private Map<String, Integer> scoresByPlayerId;

  private Round currentRound;     // nullable if no active round yet

  private Instant createdAt;
  private Instant lastUpdatedAt;
}
```

### 2.4 Round Entity

```java
public class Round {

  private int roundNumber;
  private String targetWord; // never sent to client

  private int maxAttempts; // e.g. 6
  private Map<String, List<Guess>> guessesByPlayerId;

  private Map<String, RoundPlayerStatus> statusByPlayerId;

  private boolean finished;
  private Instant startedAt;
  private Instant finishedAt;
}
```

### 2.5 Guess & LetterResult

```java
public class Guess {

  private String playerId;
  private String word;
  private List<LetterResult> letters;
  private int attemptNumber; // 1-based attempt index for that player
  private Instant createdAt;
}

public class LetterResult {

  private char letter;
  private LetterStatus status;
}
```

---

## 3. Repository Layer (H2 + Spring Data JPA)

Use **Spring Data JPA** with **H2 in-memory** DB for dev.  
No Java `Map`s, no files – everything persisted via entities & repositories so switching to a real
DB later is just a property change.

### 3.1 JPA Entities

#### `RoomEntity`

```java

@Entity
@Table(name = "rooms")
public class RoomEntity {

  @Id
  private String id; // (e.g. UUID string)

  @Enumerated(EnumType.STRING)
  private Language language;

  @Enumerated(EnumType.STRING)
  private RoomStatus status;

  // 0–2 players, stored as simple strings
  @ElementCollection
  @CollectionTable(name = "room_players", joinColumns = @JoinColumn(name = "room_id"))
  @Column(name = "player_id")
  private Set<String> playerIds = new HashSet<>();

  @ElementCollection
  @CollectionTable(name = "room_scores", joinColumns = @JoinColumn(name = "room_id"))
  @MapKeyColumn(name = "player_id")
  @Column(name = "score")
  private Map<String, Integer> scoresByPlayerId = new HashMap<>();

  @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  private List<RoundEntity> rounds = new ArrayList<>();

  private Integer currentRoundNumber; // FK-like pointer into rounds list

  private Instant createdAt;
  private Instant lastUpdatedAt;
}
```

#### `RoundEntity`

```java

@Entity
@Table(name = "rounds")
public class RoundEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private int roundNumber;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "room_id", nullable = false)
  private RoomEntity room;

  private String targetWord; // never exposed to client

  private int maxAttempts;

  @ElementCollection
  @CollectionTable(name = "round_player_status", joinColumns = @JoinColumn(name = "round_id"))
  @MapKeyColumn(name = "player_id")
  @Column(name = "status")
  @Enumerated(EnumType.STRING)
  private Map<String, RoundPlayerStatus> statusByPlayerId = new HashMap<>();

  private boolean finished;

  private Instant startedAt;
  private Instant finishedAt;

  @OneToMany(mappedBy = "round", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  private List<GuessEntity> guesses = new ArrayList<>();
}
```

#### `GuessEntity`

```java

@Entity
@Table(name = "guesses")
public class GuessEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "round_id", nullable = false)
  private RoundEntity round;

  private String playerId;

  private String word;

  private int attemptNumber;

  private Instant createdAt;

  // letter-by-letter status
  @ElementCollection
  @CollectionTable(name = "guess_letters", joinColumns = @JoinColumn(name = "guess_id"))
  private List<LetterResultEmbeddable> letters = new ArrayList<>();
}
```

#### `LetterResultEmbeddable`

```java

@Embeddable
public class LetterResultEmbeddable {

  @Column(name = "letter")
  private char letter;

  @Enumerated(EnumType.STRING)
  @Column(name = "status")
  private LetterStatus status;
}
```

#### `DictionaryWordEntity` (for language + word list)

```java

@Entity
@Table(name = "dictionary_words")
public class DictionaryWordEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  private Language language;

  // e.g. ANSWER or ALLOWED
  @Enumerated(EnumType.STRING)
  private DictionaryWordType type;

  @Column(nullable = false)
  private String word;
}

public enum DictionaryWordType {
  ANSWER,
  ALLOWED
}
```

### 3.2 Spring Data Repositories

#### Room

```java
public interface RoomJpaRepository extends JpaRepository<RoomEntity, String> {

}
```

#### Round

```java
public interface RoundJpaRepository extends JpaRepository<RoundEntity, Long> {

  Optional<RoundEntity> findByRoomIdAndRoundNumber(String roomId, int roundNumber);

  List<RoundEntity> findByRoomIdOrderByRoundNumberAsc(String roomId);
}
```

#### Guess

```java
public interface GuessJpaRepository extends JpaRepository<GuessEntity, Long> {

  List<GuessEntity> findByRoundIdOrderByAttemptNumberAsc(Long roundId);

  List<GuessEntity> findByRoundIdAndPlayerIdOrderByAttemptNumberAsc(Long roundId, String playerId);
}
```

#### Dictionary

```java
public interface DictionaryWordJpaRepository extends JpaRepository<DictionaryWordEntity, Long> {

  List<DictionaryWordEntity> findByLanguageAndType(Language language, DictionaryWordType type);
}
```

Repository-backed dictionary implementation:

```java

@Repository
public class DatabaseDictionaryRepository implements DictionaryRepository {

  private final DictionaryWordJpaRepository jpaRepository;

  @Override
  public Set<String> getAllowedGuesses(Language language) {
    return jpaRepository.findByLanguageAndType(language, DictionaryWordType.ALLOWED)
        .stream()
        .map(DictionaryWordEntity::getWord)
        .collect(Collectors.toUnmodifiableSet());
  }

  @Override
  public Set<String> getAnswerWords(Language language) {
    return jpaRepository.findByLanguageAndType(language, DictionaryWordType.ANSWER)
        .stream()
        .map(DictionaryWordEntity::getWord)
        .collect(Collectors.toUnmodifiableSet());
  }
}
```

### 3.3 H2 In-Memory Configuration (dev)

Already configured in `application-dev.yml`.

---

## 4. Service Layer & Business Logic

Use `@Service` classes for all game logic.

### 4.1 RoomService

**Responsibilities:**

- Creating rooms.
- Joining rooms (max 2 players).
- Ensuring a round exists when a room is full.
- Delegating guess handling to `GameService`.
- Managing infinite round cycle.

**Suggested interface:**

```java
public interface RoomService {

  Room createRoom(Language language, String creatorPlayerId);

  Room joinRoom(String roomId, String playerId);

  Room getRoom(String roomId);
}
```

Implementation notes:

- `createRoom`:
    - Generate `RoomId` (UUID or simple random).
    - Set language.
    - Set `status = WAITING_FOR_PLAYERS`.
    - Add creator to `playerIds`.
    - Initialize scores map with `0` for that player.
    - `currentRound = null` initially.
    - Save via repository.
- `joinRoom`:
    - Validate room exists, not CLOSED.
    - Validate players count < 2.
    - Add player to `playerIds` (if not already).
    - Initialize score if not present.
    - When `playerIds.size() == 2`:
        - Create first `Round` using `GameService.startNewRound`.
        - Set `status = IN_PROGRESS`.
    - Save and return.

### 4.2 GameService

**Responsibilities:**

- Evaluate guesses (Wordle logic).
- Validate word (length, in dictionary).
- Update round & scores.
- Start new rounds when needed.

**Suggested interface:**

```java
public interface GameService {

  Room handleGuess(String roomId, String playerId, String rawWord);
}
```

Implementation flow for `handleGuess`:

1. Load room.
2. Validate:
    - Room exists.
    - Room status is `IN_PROGRESS`.
    - `playerId` is in `room.playerIds`.
3. Ensure `Room.currentRound` is not null; if null, start a new round.
4. Normalize guess (trim, uppercase).
5. Validate word:
    - Correct length.
    - In dictionary.
6. Build `Guess` entity and evaluate letters.
7. Update round:
    - Append guess.
    - Update player status.
8. If round finished:
    - Update scores (`+1` per winner).
    - Mark round finished.
    - Start next round automatically.
9. Save updated state.
10. Return updated `Room`.

---

## 5. REST API Design

Create a `@RestController` for `/api/rooms`.

### 5.1 DTOs (API-level)

```java
public record CreateRoomRequest(
    String playerId,
    String language
) {

}

public record JoinRoomRequest(
    String playerId
) {

}

public record SubmitGuessRequest(
    String playerId,
    String word
) {

}
```

```java
public record RoomDto(
    String id,
    String language,
    String status,
    List<String> players,
    Map<String, Integer> scores,
    RoundDto currentRound
) {

}

public record RoundDto(
    int roundNumber,
    int maxAttempts,
    Map<String, List<GuessDto>> guessesByPlayerId,
    Map<String, String> statusByPlayerId,
    boolean finished
) {

}

public record GuessDto(
    String word,
    List<LetterResultDto> letters,
    int attemptNumber
) {

}

public record LetterResultDto(
    char letter,
    String status
) {

}
```

```java
public record GuessResponse(
    RoomDto room
) {

}
```

### 5.2 Endpoints

```java

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

  @PostMapping
  public ResponseEntity<RoomDto> createRoom(@RequestBody CreateRoomRequest request) {
  }

  @PostMapping("/{roomId}/join")
  public ResponseEntity<RoomDto> joinRoom(
      @PathVariable String roomId,
      @RequestBody JoinRoomRequest request
  ) {
  }

  @GetMapping("/{roomId}")
  public ResponseEntity<RoomDto> getRoom(@PathVariable String roomId) {
  }

  @PostMapping("/{roomId}/guess")
  public ResponseEntity<GuessResponse> submitGuess(
      @PathVariable String roomId,
      @RequestBody SubmitGuessRequest request
  ) {
  }
}
```

---

## 6. WebSocket (Events Only, No Guess Submission)

Use Spring’s STOMP support.

### 6.1 Configuration

```java

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.enableSimpleBroker("/topic");
    registry.setApplicationDestinationPrefixes("/app");
  }
}
```

### 6.2 Events

Event envelope:

```java
public record RoomEvent(
    String type,
    Object payload
) {

}
```

Publisher:

```java

@Service
public class RoomEventPublisher {

  private final SimpMessagingTemplate messagingTemplate;

  public void publish(String roomId, RoomEvent event) {
    messagingTemplate.convertAndSend("/topic/rooms/" + roomId, event);
  }
}
```

Send on join, round start/end, score updates, etc.

---

## 7. Dictionary & Language Handling

- Only **Italian** for now.
- Word length consistent across lists.
- Validation uses dictionary repository.
- Target words come from `ANSWER` set.

Random selection example:

```java
String randomTargetWord(Language language) {
  var answers = dictionaryRepository.getAnswerWords(language);
  return answers.stream().skip(ThreadLocalRandom.current().nextInt(answers.size()))
      .findFirst().orElseThrow();
}
```

---

## 8. Scoring Rules

- After each round:
    - If `WON` → `+1` point.
    - If `LOST` → `0`.
- No bonuses (can extend later).

---

## 9. Error Handling

### 9.1 Error DTO

```java
public record ErrorResponse(
    String code,
    String message // user-friendly description of the error
) {

}
```

### 9.2 Exceptions

```java
public class RoomNotFoundException extends RuntimeException {

}

public class RoomFullException extends RuntimeException {

}

public class PlayerNotInRoomException extends RuntimeException {

}

public class InvalidGuessException extends RuntimeException {

  private final String code;
}
```

`@RestControllerAdvice` maps to HTTP codes.

- 404 → Room not found
- 409 → Room full
- 403 → Player not in room
- 400 → Guess invalid

---

## 10. Misc Implementation Notes

- **Mapping**: use `RoomMapper` to convert entities to DTOs (never expose `targetWord`).
- **Thread-safety**: room updates should be atomic; consider per-room locking.
- **Config**: make `maxAttempts` configurable.

