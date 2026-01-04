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
3. Run: `mvn spring-boot:run`

Service starts on `http://localhost:8088` by default.

## Running profiles

- `dev`: uses H2 in-memory database (no external DB required)
- `prod`: uses PostgreSQL (see [application-prod.yaml](src/main/resources/application-prod.yaml))

Select the profile with the `PROFILE` env var (defaults to `prod`).

## Environment variables

| Variable      | Description                              | Default |
|---------------|------------------------------------------|---------|
| `PORT`        | HTTP server port                         | `8088`  |
| `PROFILE`     | Spring profile (`dev` / `prod`)          | `prod`  |
| `DB_PORT`     | PostgreSQL port (used by `prod` profile) | `null`  |
| `DB_USER`     | PostgreSQL username                      | `null`  |
| `DB_PASSWORD` | PostgreSQL password                      | `null`  |

Notes:

- In `prod`, the JDBC URL is configured
  in [application-prod.yaml](src/main/resources/application-prod.yaml) and expects `DB_PORT`,
  `DB_USER`, and `DB_PASSWORD`.
- In `dev`, the H2 console is enabled.

## API

- `POST /api/v1/rooms` – create a room
- `POST /api/v1/rooms/{roomId}/join` – join a room
- `GET /api/v1/rooms/{roomId}` – fetch room state
- `POST /api/v1/rooms/{roomId}/guess` – submit a guess

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

## Docker

Build:

```shell
docker build -t wordle-duel-service .
```

Run:

```shell
docker run --rm -p 8088:8088 --env-file .env wordle-duel-service
```
