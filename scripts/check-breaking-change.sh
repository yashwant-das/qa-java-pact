#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$root"
mkdir -p target
log=target/breaking-change.log
if ./mvnw -B -ntp test -Dcatalog.priceShape=object >"$log" 2>&1; then
  echo "FAIL: incompatible provider unexpectedly passed" >&2
  exit 1
fi
if grep -Fq '$.products[0].price' "$log" && grep -Fq 'to be a number' "$log" \
    && grep -Eq 'Tests run: [0-9]+, Failures: 1, Errors: 0, Skipped: 0' "$log"; then
  echo "Expected contract failure confirmed: object price violates numeric price contract"
else
  tail -n 60 "$log" >&2
  echo "FAIL: build failed without the expected contract mismatch" >&2
  exit 1
fi
