# Native Image Workflow

This project supports building and validating a GraalVM native executable with Maven.

## Requirements

- GraalVM JDK 21 with `native-image` installed and available on `PATH`
- Maven 3.9+

## Build the Native Executable

```shell
mvn -Pnative -DskipTests package
```

## Run native app

```shell
set -a; source .env; set +a; ./target/wordle-duel-service
```

## Record Metadata with the Tracing Agent

Some native issues only appear when real application flows execute at runtime, especially OAuth2
login, session serialization, WebSocket setup, and other dynamic behavior. Use the GraalVM tracing
agent to record metadata from a normal JVM run before rebuilding the native image.

Start the app on the JVM with the tracing agent attached:

```shell
set -a; source .env; set +a; mvn -Pnative-record -DskipTests -DskipNativeBuild=true package exec:exec@java-agent
```

Run the app flows, then stop it and persist the recorded metadata:

```shell
mvn -Pnative-record resources:copy-resources@copy-native-agent-output
```

Rebuild the native image after recording metadata:

```shell
mvn -Pnative -DskipTests package
```

## Recommended Development Loop

1. Build and run on the JVM first.
2. Use the tracing agent while exercising real user flows.
3. Copy the recorded metadata into `src/main/resources/META-INF/native-image`.
4. Rebuild the native executable.
5. Test the native binary.
