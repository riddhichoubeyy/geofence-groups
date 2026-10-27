#!/usr/bin/env sh
# Lightweight Gradle bootstrap for this repository. Downloads the pinned Gradle
# distribution when absent; no wrapper JAR is required. Requires curl or wget + unzip.
set -eu
GRADLE_VERSION=8.9
GRADLE_USER_HOME=${GRADLE_USER_HOME:-"$HOME/.gradle"}
DIST_ROOT="$GRADLE_USER_HOME/orbit-bootstrap/gradle-$GRADLE_VERSION"
GRADLE_BIN="$DIST_ROOT/gradle-$GRADLE_VERSION/bin/gradle"
ZIP="$GRADLE_USER_HOME/orbit-bootstrap/gradle-$GRADLE_VERSION-bin.zip"
URL="https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"

if [ ! -x "$GRADLE_BIN" ]; then
  mkdir -p "$GRADLE_USER_HOME/orbit-bootstrap"
  TMP="$ZIP.tmp"
  rm -f "$TMP"
  if command -v curl >/dev/null 2>&1; then
    curl --fail --location --retry 2 --output "$TMP" "$URL"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$TMP" "$URL"
  else
    echo "Orbit needs curl or wget to download Gradle $GRADLE_VERSION." >&2
    exit 1
  fi
  mv "$TMP" "$ZIP"
  if ! command -v unzip >/dev/null 2>&1; then
    echo "Orbit needs unzip to extract Gradle $GRADLE_VERSION." >&2
    exit 1
  fi
  rm -rf "$DIST_ROOT"
  mkdir -p "$DIST_ROOT"
  unzip -q "$ZIP" -d "$DIST_ROOT"
fi
exec "$GRADLE_BIN" "$@"
