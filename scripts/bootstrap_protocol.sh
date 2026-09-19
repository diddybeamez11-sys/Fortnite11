#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TARGET="$ROOT/relay/Protocol"
BRANCH="${1:-3.0}"

ensure_protocol() {
  test -f "$TARGET/settings.gradle.kts" || return 1
  test -f "$TARGET/VERSIONS.md" || return 1
  grep -q 'Bedrock_v800' "$TARGET/VERSIONS.md" || return 1
  test -f "$TARGET/bedrock-codec/src/main/java/org/cloudburstmc/protocol/bedrock/codec/v800/Bedrock_v800.java" || return 1
}

if ensure_protocol; then
  echo "relay/Protocol already contains Bedrock_v800 (Minecraft 1.21.80)"
  exit 0
fi

mkdir -p "$ROOT/relay"
rm -rf "$TARGET"
git clone --depth 1 --branch "$BRANCH" https://github.com/CloudburstMC/Protocol.git "$TARGET"
rm -rf "$TARGET/.git"

if ! ensure_protocol; then
  echo "ERROR: CloudburstMC/Protocol branch $BRANCH does not contain Bedrock_v800 (Minecraft 1.21.80)." >&2
  exit 1
fi

echo "Protocol source installed from CloudburstMC/Protocol branch $BRANCH"
