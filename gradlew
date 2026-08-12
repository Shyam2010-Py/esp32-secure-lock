#!/usr/bin/env sh
# Minimal gradlew wrapper. The gradle-wrapper.jar is a binary that
# cannot be committed via plain text. To bootstrap it, run:
#   gradle wrapper --gradle-version 8.5
# from the project root once you have a system Gradle installed.
DIR="$(cd "$(dirname "$0")" && pwd)"
exec java -classpath "$DIR/gradle/wrapper/gradle-wrapper.jar" \
  org.gradle.wrapper.GradleWrapperMain "$@"
