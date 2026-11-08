#!/usr/bin/env bash
# Checks that a release tag matches the version in app/build.gradle.kts.
#
#   scripts/check-version.sh v1.0.0-rc.1
#
# versionName must equal the tag without the leading "v". versionCode must follow the project formula:
#   (major * 10000 + minor * 100 + patch) * 10 + b
# where b is the release candidate number (1 to 8) or 9 for a final release. Google Play needs the code
# to go up with every upload; this formula keeps rc.1 < rc.2 < final for the same version.
set -euo pipefail

tag="${1:?usage: check-version.sh <tag, e.g. v1.0.0-rc.1>}"
gradle_file="$(dirname "$0")/../app/build.gradle.kts"

if [[ ! "$tag" =~ ^v([0-9]+)\.([0-9]+)\.([0-9]+)(-rc\.([1-8]))?$ ]]; then
  echo "Tag '$tag' is not vMAJOR.MINOR.PATCH or vMAJOR.MINOR.PATCH-rc.N (N from 1 to 8)" >&2
  exit 1
fi
major="${BASH_REMATCH[1]}"; minor="${BASH_REMATCH[2]}"; patch="${BASH_REMATCH[3]}"; rc="${BASH_REMATCH[5]:-9}"
expected_code=$(( (major * 10000 + minor * 100 + patch) * 10 + rc ))
expected_name="${tag#v}"

actual_code="$(sed -nE 's/^[[:space:]]*versionCode = ([0-9]+).*/\1/p' "$gradle_file")"
actual_name="$(sed -nE 's/^[[:space:]]*versionName = "([^"]+)".*/\1/p' "$gradle_file")"

status=0
if [[ "$actual_name" != "$expected_name" ]]; then
  echo "versionName is '$actual_name' but the tag says '$expected_name'" >&2; status=1
fi
if [[ "$actual_code" != "$expected_code" ]]; then
  echo "versionCode is '$actual_code' but the formula gives $expected_code for $tag" >&2; status=1
fi
[[ $status -eq 0 ]] && echo "OK: $tag = versionName $actual_name, versionCode $actual_code"
exit $status
