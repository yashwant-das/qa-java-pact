# qa-java-pact implementation plan

> **For agentic workers:** Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build a runnable Java contract-testing learning project.

**Architecture:** Two Maven modules, independent client/provider models, generated Pact contracts, loopback provider verification, file-based WireMock exercises and opt-in broker operations.

**Tech Stack:** Java 21, Maven, JUnit 5, Pact-JVM, Jackson, WireMock.

**Spec:** ../specs/2026-11-07-qa-java-pact-design.md

## Global constraints

- Java 21 and Maven; `com.qaframework.pact` namespace.
- No Falabella code or payloads; only fictional products.
- Default verification requires no cloud credentials.
- Do not create a GitHub remote or publish anything as part of local setup.

## Review focus

- Query encoding and invalid page arguments must not change request semantics.
- Missing or incompatible price fields must fail parsing, while extra fields remain compatible.
- Network delay and HTTP failures must surface as client failures.
- Missing consumer contracts must fail provider verification rather than pass zero tests.
- Cloud scripts must fail before publishing if version, branch or credentials are missing.

## Task 1: Runnable consumer/provider contract

**Files:** root and module POMs; `consumer/src/`; `provider/src/`.

**Interfaces:** `CatalogClient.search(String, int)` returns a product page; `CatalogServer(int, String)` exposes `start()`, `port()` and `close()`.

- [x] Write consumer Pact tests and provider verification tests.
- [x] Run the build and observe missing implementation fail compilation.
- [x] Implement a loopback catalog server and real HTTP consumer.
- [x] Generate and verify first-page and exhausted-page contracts.

## Task 2: Robustness and mock scenarios

**Files:** client robustness tests; `wiremock/`; `compose.yaml`; `mockoon/`.

**Interfaces:** Task 1 client consumes mock response files through actual HTTP.

- [x] Write tests for encoded queries, errors, timeouts and incompatible payloads; observe failures.
- [x] Add parsing/validation and fixture mappings.
- [x] Run all tests and run the standalone WireMock process smoke check.

## Task 3: PactFlow workflow and learning materials

**Files:** broker verification test; `scripts/`; `.github/`; README; maintained guides; hooks.

**Interfaces:** Contracts from Task 1 are published as StorefrontClient -> CatalogApi; broker verification records CatalogApi version and branch.

- [x] Add guarded CLI scripts and test their missing-configuration behavior.
- [x] Add SHA-pinned credential-free CI and weekly dependency updates.
- [x] Add setup, break/fix, mobile release and optional Mockoon exercises.
- [x] Run clean verification, lint, script guards, standalone smoke and deliberate breaking-provider verification.
- [x] Review the complete project and record unavailable live checks.
