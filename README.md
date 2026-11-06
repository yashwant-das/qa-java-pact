# API Contract Testing with Pact

Tests a fictional ecommerce API with Pact-JVM, verifies the real provider, and demonstrates PactFlow release compatibility alongside WireMock fault scenarios.

[![CI](https://github.com/yashwant-das/qa-java-pact/actions/workflows/ci.yml/badge.svg?branch=main&event=push)](https://github.com/yashwant-das/qa-java-pact/actions/workflows/ci.yml)
[![CodeQL](https://github.com/yashwant-das/qa-java-pact/actions/workflows/codeql.yml/badge.svg?branch=main&event=push)](https://github.com/yashwant-das/qa-java-pact/actions/workflows/codeql.yml)
![Java 21](https://img.shields.io/badge/Java-21-ED8B00)
![Pact-JVM](https://img.shields.io/badge/contracts-Pact--JVM-00A86B)
![WireMock](https://img.shields.io/badge/mocks-WireMock-4B32C3)
![PactFlow](https://img.shields.io/badge/broker-PactFlow-126E82)
![License MIT](https://img.shields.io/badge/license-MIT-blue)

## Why it exists

A passing test against a saved JSON response does not prove the API still returns that response. This project makes the difference observable:

- Consumer tests call the actual Java HTTP client and generate Pact contracts.
- Provider tests replay those contracts against a real, independent catalog API.
- A deliberate numeric-price-to-object change fails provider verification.
- WireMock fixtures exercise pagination, slow responses, service errors and incompatible payloads.
- Opt-in PactFlow commands publish contracts/results and check supported application versions.

All products and services are fictional. No Falabella code, data or infrastructure is used.

## Architecture

```mermaid
flowchart LR
    CT["Consumer contract tests"] --> C["CatalogClient"]
    C --> PM["Pact mock API"]
    CT --> PF["Generated Pact file"]
    PF --> PV["Provider verifier"]
    PV --> API["Real CatalogServer"]
    RT["Client robustness tests"] --> C
    C --> WM["WireMock scenarios"]
    PF -. "explicit publish" .-> B["PactFlow"]
    B -. "broker profile" .-> PV
    PV -. "verification results" .-> B
```

### Project structure

```text
qa-java-pact/
├── pom.xml                              # Parent build, modules and pinned dependencies
├── mvnw / mvnw.cmd                      # Maven Wrapper; checksum pinned in .mvn/
├── consumer/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/qaframework/pact/consumer/
│       │   └── CatalogClient.java       # Actual HTTP client and response parsing
│       └── test/java/com/qaframework/pact/consumer/
│           ├── CatalogClientPactTest.java        # Generates consumer contracts
│           └── CatalogClientRobustnessTest.java  # Fault handling via WireMock
├── provider/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/qaframework/pact/provider/
│       │   └── CatalogServer.java       # Independent fictional catalog API
│       └── test/java/com/qaframework/pact/provider/
│           ├── CatalogServerTest.java   # API functional behaviour
│           ├── LocalVerificationTest.java   # Verifies locally generated contracts
│           └── BrokerVerificationTest.java  # Opt-in PactFlow verification
├── wiremock/
│   ├── mappings/                        # Request matching, delays and HTTP errors
│   └── __files/catalog/                 # Pagination and incompatible-price fixtures
├── postman/
│   └── catalog.postman_collection.json   # Importable requests, examples and assertions
├── mockoon/
│   └── catalog.openapi.json             # Optional desktop import exercise
├── scripts/
│   ├── pactflow.sh                      # Explicit publishing and release operations
│   ├── test-pactflow-guards.sh           # Rejects missing cloud configuration
│   └── check-breaking-change.sh         # Confirms an incompatible provider fails
├── docs/
│   ├── architecture.md                  # Boundaries and contract lifecycle
│   ├── development.md                   # Setup, commands and standards audit
│   ├── learning-path.md                 # Guided exercises
│   └── superpowers/                     # Historical design and verification records
├── .github/
│   ├── workflows/                       # CI and Java CodeQL scanning
│   └── dependabot.yml                   # Weekly dependency updates
├── compose.yaml                         # Standalone WireMock on port 8080
└── .pre-commit-config.yaml               # Local quality hooks
```

Generated contracts live in `consumer/target/pacts/`; test and analysis reports live under each module's `target/`. These build outputs are ignored by Git.

### Tech stack

| Layer | Tool | Version source |
| --- | --- | --- |
| Runtime | Java 21 | [.java-version](.java-version), compiler release in [pom.xml](pom.xml) |
| Build | Maven Wrapper | [.mvn/wrapper/maven-wrapper.properties](.mvn/wrapper/maven-wrapper.properties), distribution checksum pinned |
| Tests | JUnit 5, Pact-JVM | [pom.xml](pom.xml) |
| JSON | Jackson | [pom.xml](pom.xml) |
| Mocks | WireMock standalone | [compose.yaml](compose.yaml), same version in Maven tests |
| Quality | Spotless, PMD, pre-commit, markdownlint | [pom.xml](pom.xml), [.pre-commit-config.yaml](.pre-commit-config.yaml) |

## Quickstart

Prerequisites: JDK 21 and network access for the first dependency download. Set `JAVA_HOME` to JDK 21; confirm the Java runtime shown by `./mvnw -v`. Docker and PactFlow credentials are optional.

From the repository root:

```bash
./mvnw clean verify
```

Expected: `BUILD SUCCESS`, with 16 passing tests/invocations and no skipped tests. The consumer writes `consumer/target/pacts/StorefrontClient-CatalogApi.json`; the provider verifies its two interactions. No external API, PactFlow account or Docker daemon is used by this build.

The sample has one product per page: page 1 is a moisturiser, page 2 is sunscreen, and page 3 is empty.

## Usage

Run the mock API, then request a fixture:

```bash
docker compose up -d
curl 'http://localhost:8080/products?category=skincare&page=1'
docker compose down
```

Run the real sample API in a separate terminal:

```bash
./mvnw -pl consumer -am install -DskipTests
./mvnw -pl provider compile exec:java
```

It listens on `http://127.0.0.1:8081`. Stop it with Ctrl+C. See the [learning path](docs/learning-path.md) for pagination, errors, the breaking-change exercise, Mockoon and PactFlow.

### Explore with Postman

Import [the Postman collection](postman/catalog.postman_collection.json), then open **Provider API → 01 · Products page 1** and choose **Send**. Collection variables already target the real provider on port 8081 and WireMock on port 8080. Start the server for the folder you want to explore, or start both to run all 18 requests.

The [API and Postman guide](docs/api.md) covers server startup, query parameters, response fields, error cases, mock-only scenarios and the Collection Runner.

## Verification

| Command | What it verifies |
| --- | --- |
| `./mvnw clean verify` | Actual consumer, local provider, WireMock mappings, Java formatting and PMD |
| `bash scripts/check-breaking-change.sh` | Provider verification rejects an object where the consumer requires a numeric price |
| `bash scripts/test-pactflow-guards.sh` | Cloud commands reject missing configuration before invoking account operations |
| `pre-commit run --all-files` | Maintained Markdown, Java formatting, YAML/JSON/XML and text hygiene |
| `bash scripts/pactflow.sh verify` | Live broker contracts against the local provider; explicitly publishes results |

The first three commands require no cloud account. Maven tests use loopback HTTP servers on random ports. The first Maven and hook runs download their pinned tools; subsequent runs can use cached dependencies. CI runs local verification and the negative exercise. Reports are under each module's `target/surefire-reports/`, with PMD results in `target/pmd.xml`.

PactFlow live operations and Mockoon desktop behaviour require separate setup; a local passing build does not prove either integration has run.

## Documentation

- [API and Postman guide](docs/api.md): endpoint reference, import steps, examples and collection runs.
- [Architecture](docs/architecture.md): responsibilities, lifecycle and test boundaries.
- [Development](docs/development.md): setup, configuration, cloud commands, checks and limitations.
- [Learning path](docs/learning-path.md): step-by-step exercises and expected results.
- [Historical scope and plan](docs/superpowers/specs/2026-11-07-qa-java-pact-design.md): original design decisions, not current setup instructions.

## License

MIT. See [LICENSE](LICENSE).
