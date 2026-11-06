#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "$0")/.." && pwd)"
for operation in publish verify can-i-deploy record-release record-deployment record-support-ended; do
  variables="PACT_BROKER_BASE_URL PACT_BROKER_TOKEN PACT_VERSION"
  case "$operation" in
    publish|verify) variables="$variables PACT_BRANCH" ;;
    *) variables="$variables PACTICIPANT PACT_ENVIRONMENT" ;;
  esac
  for variable in $variables; do
    output="$(env PACT_BROKER_BASE_URL=https://example.invalid PACT_BROKER_TOKEN=not-a-real-token \
      PACT_VERSION=test-version PACT_BRANCH=main PACTICIPANT=CatalogApi PACT_ENVIRONMENT=learning \
      env -u "$variable" bash "$root/scripts/pactflow.sh" "$operation" 2>&1)" && {
      echo "FAIL: $operation accepted missing $variable" >&2
      exit 1
    }
    case "$output" in
      *"$variable"*) ;;
      *) echo "FAIL: $operation did not reject missing $variable: $output" >&2; exit 1 ;;
    esac
  done
done
echo "PactFlow configuration guards passed (28 missing-variable cases)"
