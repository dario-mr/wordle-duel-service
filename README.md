# Wordle Duel Service

Backend service for the Wordle Duel game.

Built with Spring Boot (Java 21) and provides:

- REST API for room lifecycle and guesses
- WebSocket (STOMP) events for live room updates
- OpenAPI/Swagger UI documentation
- Actuator endpoints (health / info / prometheus)
- Liquibase migrations (PostgreSQL in `prod`, H2 in-memory in `dev`)

## Requirements

- Java 21
- Maven 3.9+
- Docker (optional, to run via container)

## Quick start (local)

The app reads configuration from environment variables and also supports a local `.env` file.

1. Copy the provided example file: `cp .env.example .env`
2. Set `PROFILE=dev` for local development.
3. Set `WORDLE_JWT_SECRET`, `WORDLE_GOOGLE_CLIENT_ID`, and `WORDLE_GOOGLE_CLIENT_SECRET`.
4. Start Redis (required for OAuth2 login session state and job synchronization):

   ```shell
   docker run --rm -d --name wordle-duel-service-redis -p 6379:6379 redis:7-alpine
   ```

5. Run: `mvn spring-boot:run`

Service starts on `http://localhost:8088` by default.

## Running profiles

- `dev`: uses H2 in-memory database (no external DB required)
- `prod`: uses PostgreSQL (see [application-prod.yaml](src/main/resources/application-prod.yaml))

Select the profile with the `PROFILE` env var (defaults to `prod`).

## Environment variables

| Variable                      | Description                              | Default |
|-------------------------------|------------------------------------------|---------|
| `PORT`                        | HTTP server port                         | `8088`  |
| `PROFILE`                     | Spring profile (`dev` / `prod`)          | `prod`  |
| `DB_PORT`                     | PostgreSQL port (used by `prod` profile) | `null`  |
| `DB_USER`                     | PostgreSQL username                      | `null`  |
| `DB_PASSWORD`                 | PostgreSQL password                      | `null`  |
| `WORDLE_GOOGLE_CLIENT_ID`     | Google OAuth2 client id                  | `null`  |
| `WORDLE_GOOGLE_CLIENT_SECRET` | Google OAuth2 client secret              | `null`  |
| `WORDLE_JWT_SECRET`           | JWT signing secret (HMAC)                | `null`  |
| `SPRING_DATA_REDIS_HOST`      | Redis host                               | `null`  |
| `SPRING_DATA_REDIS_PORT`      | Redis port                               | `null`  |

Notes:

- In `prod`, the JDBC URL is configured
  in [application-prod.yaml](src/main/resources/application-prod.yaml) and expects `DB_PORT`,
  `DB_USER`, and `DB_PASSWORD`.
- In `dev`, the H2 console is enabled and no DB env vars are required.

## Authentication

This service uses Google OAuth2 login to issue a long-lived refresh token (stored as an HttpOnly
cookie) and short-lived access tokens (Bearer JWT).

OAuth2 login uses a server-side session to persist the authorization request/state across the
redirect. For horizontal scalability, sessions are stored in Redis; in local development you should
run Redis (see Quick start).

- OAuth2 login entrypoint: `GET /oauth2/authorization/google`
- Refresh access token: `POST /auth/refresh` (returns a JWT; requires CSRF)
- Logout: `POST /auth/logout`

### Obtain an access token via Swagger UI

1. Open Swagger UI: `http://localhost:8088/swagger-ui/index.html`
2. Complete Google login in the same browser by visiting:
   `http://localhost:8088/oauth2/authorization/google`
3. Back in Swagger UI, call `POST /auth/refresh` to get an `accessToken`.
4. Click Swagger's "Authorize" button and paste the token as a `Bearer` token.

(Non-browser clients must send `X-WD-XSRF-TOKEN` header with the value from the `WD-XSRF-TOKEN`
cookie)

## Redis usage

Redis is used for:

- OAuth2 login HTTP session storage (Spring Session) to persist authorization request/state across
  the redirect in a horizontally scalable way.
- Scheduler job synchronization (ShedLock) so only one instance executes scheduled cleanup jobs.

Notes:

- `SPRING_DATA_REDIS_HOST` / `SPRING_DATA_REDIS_PORT` configure the Redis connection.
- In Docker Compose deployments, point `SPRING_DATA_REDIS_HOST` / `SPRING_DATA_REDIS_PORT` to the
  Redis service (e.g. `wordle-duel-service-redis:6379`).
- In local development, if you run Redis on `localhost:6379` (see Quick start), you don't need to
  set the Redis env vars.
- Room concurrency is enforced via DB row locks (not Redis).

### Stop Redis (local)

```shell
docker stop wordle-duel-service-redis
```

## API

All endpoints under `/api/v1/**` require `Authorization: Bearer <accessToken>`.

- `POST /api/v1/rooms` – create a room
- `POST /api/v1/rooms/{roomId}/join` – join a room
- `GET /api/v1/rooms/{roomId}` – fetch room state
- `POST /api/v1/rooms/{roomId}/guess` – submit a guess
- `POST /api/v1/rooms/{roomId}/ready` – mark a player as ready for the next round

## WebSocket

- STOMP endpoint: `/ws`
- Broker destination prefix: `/topic`

Room events are published to: `/topic/rooms/{roomId}`

## Docs & observability

- Swagger UI: `http://localhost:8088/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8088/v3/api-docs`
- Actuator:
    - `http://localhost:8088/actuator/health`
    - `http://localhost:8088/actuator/info`
    - `http://localhost:8088/actuator/prometheus`

## Build & test

- Run tests: `mvn test`
- Build JAR: `mvn package`
- Coverage report: `mvn -Pcoverage verify` (HTML report at `target/site/jacoco/index.html`)

## Docker

Build:

```shell
docker build -t wordle-duel-service .
```

Run:

```shell
docker run --rm -p 8088:8088 --env-file .env wordle-duel-service
```
