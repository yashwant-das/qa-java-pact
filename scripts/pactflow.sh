#!/usr/bin/env bash
# Explicit, opt-in PactFlow operations. Never print the broker token.
set -euo pipefail
root="$(cd "$(dirname "$0")/.." && pwd)"
operation="${1:-}"
case "$operation" in
  publish|verify|can-i-deploy|record-release|record-deployment|record-support-ended) ;;
  *) echo "Usage: $0 {publish|verify|can-i-deploy|record-release|record-deployment|record-support-ended}" >&2; exit 2 ;;
esac
: "${PACT_BROKER_BASE_URL:?Set PACT_BROKER_BASE_URL to your PactFlow HTTPS URL}"
: "${PACT_BROKER_TOKEN:?Set PACT_BROKER_TOKEN in your environment}"
: "${PACT_VERSION:?Set PACT_VERSION to the immutable version being tested, usually its git SHA}"
case "$PACT_BROKER_BASE_URL" in
  https://*) ;;
  *) echo "PACT_BROKER_BASE_URL must use HTTPS" >&2; exit 2 ;;
esac
case "$operation" in
  publish|verify) : "${PACT_BRANCH:?Set PACT_BRANCH to the tested application branch}" ;;
  can-i-deploy|record-release|record-deployment|record-support-ended)
    : "${PACTICIPANT:?Set PACTICIPANT to StorefrontClient or CatalogApi}"
    : "${PACT_ENVIRONMENT:?Set PACT_ENVIRONMENT to the target environment}"
    ;;
esac
cd "$root"
if [[ "$operation" == verify ]]; then
  : "${PACT_BRANCH:?Set PACT_BRANCH to the provider branch}"
  # Keep consumer generation credential-free; publish only real provider results.
  ./mvnw -B -ntp -pl consumer -am install -DskipTests
  exec ./mvnw -B -ntp -pl provider -Pbroker test \
    -Dpact.verifier.publishResults=true \
    "-Dpact.provider.version=$PACT_VERSION" "-Dpact.provider.branch=$PACT_BRANCH"
fi
command -v pact >/dev/null || { echo "Install Pact CLI 0.10.8; see docs/development.md" >&2; exit 2; }
case "$operation" in
  publish)
    : "${PACT_BRANCH:?Set PACT_BRANCH to the consumer branch}"
    [[ -s consumer/target/pacts/StorefrontClient-CatalogApi.json ]] || { echo "Run ./mvnw clean verify before publishing" >&2; exit 2; }
    exec pact broker publish consumer/target/pacts/StorefrontClient-CatalogApi.json \
      --consumer-app-version "$PACT_VERSION" --branch "$PACT_BRANCH"
    ;;
  can-i-deploy)
    : "${PACTICIPANT:?Set PACTICIPANT to StorefrontClient or CatalogApi}"
    : "${PACT_ENVIRONMENT:?Set PACT_ENVIRONMENT to the target environment}"
    exec pact broker can-i-deploy --pacticipant "$PACTICIPANT" --version "$PACT_VERSION" \
      --to-environment "$PACT_ENVIRONMENT"
    ;;
  record-release|record-deployment|record-support-ended)
    : "${PACTICIPANT:?Set PACTICIPANT to StorefrontClient or CatalogApi}"
    : "${PACT_ENVIRONMENT:?Set PACT_ENVIRONMENT to the target environment}"
    exec pact broker "$operation" --pacticipant "$PACTICIPANT" --version "$PACT_VERSION" \
      --environment "$PACT_ENVIRONMENT"
    ;;
esac
