#!/usr/bin/env sh
set -eu
if ! command -v gradle >/dev/null 2>&1; then
  echo "Gradle was not found. Install Gradle 8.x and Java 21, or open this folder in IntelliJ IDEA as a Gradle project." >&2
  exit 1
fi
exec gradle build
