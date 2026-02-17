# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this
repository.

## Build & Test Commands

```bash
mvn spring-boot:run                # Run locally (requires PROFILE=dev in .env and Redis running)
mvn test                           # Run all tests
mvn test -Dtest=RoomServiceTest    # Run a single test class
mvn test -Dtest="RoomServiceTest#methodName_condition_result"  # Run a single test method
mvn -Pcoverage verify              # Run tests with JaCoCo coverage (report: target/site/jacoco/index.html)
mvn package -DskipTests            # Build JAR without tests
```

Local Redis (required for OAuth2 sessions and ShedLock):

```bash
docker run --rm -d --name wordle-duel-service-redis -p 6379:6379 redis:7-alpine
```

## Architecture

Spring Boot 3.5 / Java 21 backend for a 2-player multiplayer Wordle game. Players share a room,
submit guesses against the same hidden word, and progress through rounds.

### Package layout (`com.dariom.wds`)

- **`domain`** — Pure domain model (Java records): `Room`, `Round`, `Player`, `Guess`,
  `LetterResult`
- **`persistence.entity`** — JPA entities (separate from domain); all columns and join columns
  explicitly named
- **`persistence.repository`** — Thin `@Repository` wrappers over Spring Data JPA interfaces; add
  pessimistic locking and exception translation
- **`persistence.repository.jpa`** — Spring Data `JpaRepository` interfaces with JPQL/`@EntityGraph`
  queries
- **`service`** — Business logic: `DomainMapper` (entity↔domain), `WordleEvaluator`
- **`service.room`** / **`service.round`** / **`service.auth`** / **`service.user`** —
  Domain-specific service layers
- **`api.v1`** — REST controllers, DTOs, request/response mappers, validators, error handling
- **`api.admin`** — Admin-only endpoints (`/admin/**`)
- **`api.auth`** — Auth endpoints (`/auth/refresh`, `/auth/logout`)
- **`config`** — Spring configuration classes (security, cache, WebSocket, ShedLock, custom
  `@ConfigurationProperties` records)
- **`job`** — Scheduled cleanup jobs with ShedLock (cluster-safe via Redis)
- **`websocket`** — STOMP event listener and event model records

### Key design patterns

- **Domain/entity separation:** Pure records in `domain` are decoupled from JPA entities.
  `DomainMapper` converts between them.
- **Pessimistic DB row locks for room mutations:** Repository wrappers use `PESSIMISTIC_WRITE` with
  configurable timeout. Lock failures become `RoomLockedException` → HTTP 409.
- **Transactional event listener for WebSocket:** Services publish `RoomEventToPublish` via
  `ApplicationEventPublisher`. `RoomEventListener` uses
  `@TransactionalEventListener(phase = AFTER_COMMIT)` so WebSocket notifications are sent only after
  successful DB commits.
- **Dual security filter chains:** API endpoints use stateless JWT (CSRF disabled). Auth/OAuth2
  endpoints use cookie-based CSRF with server-side sessions.
- **Custom `@ConfigurationProperties` records:** `WordleProperties`, `SecurityProperties`,
  `RoomLockProperties`, `RoomCleanupProperties`, `WebSocketProperties` — all scanned via
  `@ConfigurationPropertiesScan`.

### Auth flow

Google OAuth2 login → refresh token as HttpOnly cookie → `POST /auth/refresh` returns short-lived
HS256 JWT → API requests use `Authorization: Bearer <token>`. JWT carries `uid` (UUID) and `roles`
claims.

### Profiles

- **`dev`**: H2 in-memory DB (MODE=PostgreSQL), H2 console at `/h2-console`, no external DB needed
- **`prod`**: PostgreSQL (Supabase), scheduled cleanup crons active

### Database

Liquibase migrations in `src/main/resources/db/changelog/`. Schema: `wordle`. Key tables: `rooms`,
`rounds`, `guesses`, `guess_letters`, `room_players`, `round_player_status`, `dictionary_words`.

## Code Conventions

- Use `var` for local variables when the type is obvious
- Return `Optional` instead of `null`
- Prefer static imports
- Use Lombok `@RequiredArgsConstructor` for DI, `@Slf4j` for logging
- JPA entities: explicitly name all columns (`@Column(name = "...")`, `@JoinColumn(name = "...")`)
- All changes must be covered by unit tests

## Testing Conventions

- **Unit tests** (`*Test.java`): JUnit 5 + Mockito + AssertJ. Test names:
  `methodName_condition_result`. AAA pattern (Arrange / Act / Assert) with blank lines between
  sections.
- **Mocks**: `@ExtendWith(MockitoExtension.class)`, `@Mock` + `@InjectMocks`. `when(...)` uses
  matchers (`any()`); `verify(...)` asserts actual values. Don't mock mappers without dependencies.
- **Exceptions**: Use `catchThrowable(() -> ...)` from AssertJ.
- **Integration tests** (`*IT.java`): `@SpringBootTest` + `@AutoConfigureMockMvc` +
  `@Transactional`. Use `IntegrationTestHelper` for creating test users and issuing JWTs.
- **Repository tests**: `@DataJpaTest` with H2. No `EntityManager` usage. Rely on rollback for
  isolation (no `deleteAll()` in `@BeforeEach`).
- **Redis tests**: Use Testcontainers (`@Testcontainers`) for a real Redis container.
