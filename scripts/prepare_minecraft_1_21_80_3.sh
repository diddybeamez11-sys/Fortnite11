#!/usr/bin/env bash
set -euo pipefail

# Validate the exact Minecraft payload before a controlled-session build.
# This script intentionally does not modify or redistribute the Minecraft APK.
APK="${1:-}"
if [[ -z "$APK" || ! -f "$APK" ]]; then
  echo "Usage: $0 /path/to/minecraft-1.21.80.3-arm64.apk" >&2
  exit 2
fi

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
unzip -q "$APK" 'lib/arm64-v8a/libminecraftpe.so' -d "$TMP"
LIB="$TMP/lib/arm64-v8a/libminecraftpe.so"
[[ -f "$LIB" ]] || { echo "Missing lib/arm64-v8a/libminecraftpe.so" >&2; exit 1; }

SIZE="$(stat -c '%s' "$LIB")"
SHA="$(sha256sum "$LIB" | awk '{print $1}')"

EXPECTED_SIZE=247512600
EXPECTED_SHA=84cc649545f95420212038dd483896d5d53507e25a309e68bd038b0256a976ae

[[ "$SIZE" == "$EXPECTED_SIZE" ]] || { echo "Wrong libminecraftpe.so size: $SIZE" >&2; exit 1; }
[[ "$SHA" == "$EXPECTED_SHA" ]] || { echo "Wrong libminecraftpe.so SHA-256: $SHA" >&2; exit 1; }

echo "Minecraft native payload verified"
echo "version: 1.21.80.3"
echo "abi: arm64-v8a"
echo "size: $SIZE"
echo "sha256: $SHA"
