# Native Image Workflow

This project supports building and validating a GraalVM native executable with Maven.

## Requirements

- GraalVM JDK 25 with `native-image` installed and available on `PATH`
- Maven 3.9+

If you want to use the GraalVM JDK just for this project, an option is to create an alias in
`~/.zshenv`:

```shell
mvn25() {
  export JAVA_HOME=~/Portable/java/graalvm-jdk-25.0.2
  export PATH="$JAVA_HOME/bin:$PATH"
  mvn "$@"
}
```

## Build the Native Executable

```shell
mvn25 -Pnative -DskipTests package
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
set -a; source .env; set +a; mvn25 -Pnative-record -DskipTests -DskipNativeBuild=true package exec:exec@java-agent
```

Run the app flows, then stop it and persist the recorded metadata:

```shell
mvn25 -Pnative-record resources:copy-resources@copy-native-agent-output
```

Rebuild the native image after recording metadata:

```shell
mvn25 -Pnative -DskipTests package
```

## Recommended Development Loop

1. Build and run on the JVM first.
2. Use the tracing agent while exercising real user flows.
3. Copy the recorded metadata into `src/main/resources/META-INF/native-image`.
4. Rebuild the native executable.
5. Test the native binary.
