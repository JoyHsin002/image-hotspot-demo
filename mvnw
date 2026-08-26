#!/bin/sh
set -eu
if command -v mvn >/dev/null 2>&1; then exec mvn "$@"; fi
WRAPPER_VERSION=3.9.11
BASE="${TMPDIR:-/tmp}/image-hotspot-maven-$WRAPPER_VERSION"
if [ ! -x "$BASE/bin/mvn" ]; then mkdir -p "$BASE"; curl -fsSL "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$WRAPPER_VERSION/apache-maven-$WRAPPER_VERSION-bin.tar.gz" | tar -xz --strip-components=1 -C "$BASE"; fi
exec "$BASE/bin/mvn" "$@"
