#!/bin/sh
set -eu

BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROPS="$BASE_DIR/.mvn/wrapper/maven-wrapper.properties"
DIST_URL=$(sed -n 's/^distributionUrl=//p' "$PROPS")
MAVEN_VERSION=$(printf '%s' "$DIST_URL" | sed -n 's#^.*/apache-maven-\([^/]*\)-bin.zip$#\1#p')
WRAPPER_HOME="${MAVEN_USER_HOME:-${HOME:-/tmp}/.m2}/wrapper/dists/apache-maven-$MAVEN_VERSION"
MAVEN_HOME="$WRAPPER_HOME/apache-maven-$MAVEN_VERSION"

if [ ! -x "$MAVEN_HOME/bin/mvn" ]; then
  mkdir -p "$WRAPPER_HOME"
  ZIP="$WRAPPER_HOME/apache-maven-$MAVEN_VERSION-bin.zip"
  if [ ! -f "$ZIP" ]; then
    if command -v curl >/dev/null 2>&1; then
      curl -fsSL "$DIST_URL" -o "$ZIP"
    elif command -v wget >/dev/null 2>&1; then
      wget -q "$DIST_URL" -O "$ZIP"
    else
      echo "mvnw requires curl or wget to download Maven" >&2
      exit 1
    fi
  fi
  command -v unzip >/dev/null 2>&1 || {
    echo "mvnw requires unzip to install Maven" >&2
    exit 1
  }
  unzip -q -o "$ZIP" -d "$WRAPPER_HOME"
fi

exec "$MAVEN_HOME/bin/mvn" "$@"
