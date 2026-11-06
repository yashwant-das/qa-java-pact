# Development

## Local setup

Use JDK 21. The compiler emits Java 21 bytecode; the verified runtime is Homebrew OpenJDK 21 on Apple Silicon. Set `JAVA_HOME` to your JDK 21 installation, then check `./mvnw -v`. On this workstation:

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
./mvnw -v
./mvnw clean verify
```

The wrapper pins Maven and its distribution SHA-256 checksum. Dependencies and build plugins are pinned in the POMs. Dependencies come from Maven Central. Initial downloads require network access. No credentials are needed for local tests.

For hooks, install `pre-commit==4.7.0` with your preferred Python tool manager, then run:

```bash
pre-commit install
pre-commit run --all-files
```

Hooks inspect staged file types; the Java hook invokes the project-wide formatter check. CI checks all Java through Maven and all maintained Markdown with the same pinned markdownlint version. Node is tooling only; the application and tests remain Java. Formatting fixes are explicit: `./mvnw spotless:apply`.

## Configuration

| Setting | Default | Scope |
| --- | --- | --- |
| `JAVA_HOME` | Your shell's JDK | Wrapper and Java tools; use JDK 21 |
| `CATALOG_PORT` | `8081` | Real API CLI only; bound to loopback |
| `catalog.priceShape` | `number` | Provider verification teaching switch: `number` or `object` |
| `pact.verifier.publishResults` | `false` | Publishing actual broker verification results |
| `pact.provider.version` | `local` | Provider version for verification reporting |
| `pact.provider.branch` | `local` | Provider branch for reporting |
| `PACT_BROKER_BASE_URL` | Unset | Your PactFlow HTTPS account URL |
| `PACT_BROKER_TOKEN` | Unset | Token with permissions for the requested operation |
| `PACT_VERSION` | Unset | Immutable application version for cloud scripts |
| `PACT_BRANCH` | Unset | Consumer branch on publish; provider branch on verify |
| `PACTICIPANT` | Unset | `StorefrontClient` or `CatalogApi` for release checks |
| `PACT_ENVIRONMENT` | Unset | Existing PactFlow environment, such as `production` |

`.env.example` documents names only. It is not automatically loaded. Export values in your shell or configure CI secrets. Do not commit `.env`, access tokens, or logs containing credentials. Scripts require HTTPS for PactFlow.

## PactFlow setup

Create your own PactFlow account. Install the official [Pact CLI 0.10.8 release](https://github.com/pact-foundation/pact-cli/releases/tag/v0.10.8) for your platform and put the `pact` executable on PATH. Download the matching checksum asset and verify it before extracting. `pact --help` and `pact broker publish --help` should display usage. This project uses the unified `pact broker` command, not the older Ruby `pact-broker` executable.

Export the broker URL and token, and an immutable version for the code you tested:

```bash
export PACT_BROKER_BASE_URL=https://YOUR-ACCOUNT.pactflow.io
read -rs PACT_BROKER_TOKEN
export PACT_BROKER_TOKEN
export PACT_VERSION="$(git rev-parse HEAD)"
export PACT_BRANCH=main
```

Replace the URL placeholder with your account. The `read` command waits for your token without echoing it. Use the actual branch instead of `main` when working elsewhere. Make a local commit after checks pass before using its SHA; this setup does not publish or create a remote automatically.

From the root, run:

```bash
./mvnw clean verify
bash scripts/pactflow.sh publish
bash scripts/pactflow.sh verify
```

`publish` uploads the already generated contract. `verify` installs the consumer artifact without running its tests, then runs only the provider's broker verification suite. It fetches selected contracts and publishes real verification results under `PACT_VERSION` and `PACT_BRANCH`. No PactFlow command has been run on an account during initial setup.

In PactFlow, configure `StorefrontClient`'s main branch to match `PACT_BRANCH` before broker verification. The selector includes main-branch and all deployed or released/supported consumer versions. The first run must have at least one selected consumer contract. Missing selections must be fixed by publishing/configuring branches, not by ignoring empty verification.

For a first release, check the exact pair before recording either as present in an environment:

```bash
pact broker can-i-deploy --pacticipant StorefrontClient --version "$PACT_VERSION" \
  --pacticipant CatalogApi --version "$PACT_VERSION"
```

Create a `production` environment in PactFlow. After the corresponding application is actually released/deployed, record it:

```bash
PACTICIPANT=CatalogApi PACT_ENVIRONMENT=production bash scripts/pactflow.sh record-deployment
PACTICIPANT=StorefrontClient PACT_ENVIRONMENT=production bash scripts/pactflow.sh record-release
```

This sample has no real deployment: for an exercise, use an environment clearly named `learning` instead of claiming a production release. In real CI, record only after the deployment/release succeeds. A mobile app is recorded as a release because several versions may remain supported simultaneously.

For later backend changes:

```bash
PACTICIPANT=CatalogApi PACT_ENVIRONMENT=production bash scripts/pactflow.sh can-i-deploy
```

Successful verification is necessary for compatibility, and `can-i-deploy` uses recorded environment/version information to evaluate it. Track it accurately. When support for a released mobile version actually ends, set `PACT_VERSION` to that older version before using `record-support-ended`; never remove support merely to obtain a green gate.

## Checks and reports

| Check | Command | Evidence |
| --- | --- | --- |
| Compile, tests, format, PMD | `./mvnw clean verify` | Surefire XML and PMD XML under module targets |
| Format only | `./mvnw spotless:check` | Nonzero exit on changed formatting |
| Explicit format fix | `./mvnw spotless:apply` | Review changed files before committing |
| Cloud configuration guards | `bash scripts/test-pactflow-guards.sh` | Rejects 28 missing-variable cases across six operations |
| Negative contract exercise | `bash scripts/check-breaking-change.sh` | Requires a numeric-price mismatch, not just any build failure |
| Markdown/config/text | `pre-commit run --all-files` | All applicable hooks pass |
| Compose structure | `docker compose config --quiet` | Parsed configuration |

The negative exercise intentionally creates failed provider test reports. Run `./mvnw clean verify` afterward to restore normal passing reports. CI archives reports for three days. There are no cloud calls, shared staging dependencies or Docker requirements in the Java suite.

## Adding cases

Add consumer contracts only for behaviour the actual client relies on. Invoke `CatalogClient` from the test and assert decoded data. Use matchers for variable data, rather than freezing every field to its example. Add provider state setup when a new interaction needs different real data.

Use robustness tests for timeouts, HTTP errors and malformed input. Add manual WireMock mappings under `wiremock/mappings/` and JSON bodies under `wiremock/__files/`; the automated client tests load those same files. There are no runtime Java route registrations to maintain.

## Troubleshooting

- Missing generated Pact: run from the root so Maven builds the consumer first. A provider-only invocation cannot generate a missing consumer contract.
- Wrong Java runtime: inspect `JAVA_HOME` and `./mvnw -v`; `java -version` alone may refer to a different installation.
- Port 8080/8081 occupied: stop the old process. Set `CATALOG_PORT` for the provider or change the Compose host port. Automated tests use random ports.
- A 404 from WireMock: check `category` and `page`; only documented mappings are defined. An exhausted page is a 200 response with an empty list.
- Broker profile finds no pacts: check participant names, consumer main branch and which versions were published.
- `can-i-deploy` is unknown or fails: verify/publish the required pair and check environment tracking; do not suppress the result.

## Standards and verification record

Documentation profile: `complete`. Standards: [repo-standards a429dd2](https://github.com/yashwant-das/repo-standards/tree/a429dd2f2da55a4c3e1c73c7c0d1111f9cd0b9a9). Dependency versions were resolved from Maven Central metadata on 2026-11-07 and compiled/tested as a set with JDK 21. Maven Wrapper distribution checksum is pinned.

To start only the provider module, first install the parent and consumer with `./mvnw -pl consumer -am install -DskipTests`; the provider has a test-scope reactor dependency on that artifact.

PMD's hard-coded-IP warning is suppressed only on the sample server constructor: binding specifically to IPv4 loopback is deliberate and matches provider verification. Owner: project maintainer. Review when supporting external hosts or IPv6. Pact-JVM currently emits a JUnit `CloseableResource` deprecation warning; it does not skip or fail tests. Review when updating Pact/JUnit.

Local verification covers Java, generated contracts, the intentional incompatibility and fixture mappings. Live PactFlow publishing/release tracking needs the owner's account and has not been exercised. Mockoon desktop import is an optional manual exercise. Docker Compose and standalone-JAR smoke checks passed: distinct first/second products, empty final page and the configured mappings. All pre-commit hooks passed. The temporary container was stopped after verification.

### Adoption audit

The project adopts the pinned standards above. The following table records the implementation and evidence rather than treating publication as proof of compliance.

| Area | Implementation | Evidence or remaining boundary |
| --- | --- | --- |
| Documentation | `complete` profile; descriptive README title, badges, Mermaid, stack/structure, quickstart; architecture, development and learning guides | Maintained Markdown is linted; historical planning is clearly labelled and excluded |
| Structure | Separate Maven consumer/provider modules; `com.qaframework.pact` packages | Provider imports no consumer models; reactor dependency establishes test order |
| Runtime and dependencies | Java 21 compiler target, Maven Wrapper checksum, pinned libraries/plugins, Node 24 for hook tooling | Versions resolved from Maven Central and verified together locally |
| Local gates | Pinned pre-commit hooks, Spotless, PMD, Markdown/config/text checks | `pre-commit run --all-files` and `./mvnw clean verify` |
| Hosted gates | SHA-pinned Actions, least-privilege token permissions, job timeouts, three-day reports, secret-free default suite and Java CodeQL scanning | `Java contracts and quality` is the required-check candidate; enforcement is tracked separately below |
| Dependency updates | Weekly Maven, Compose and GitHub Actions updates | `.github/dependabot.yml`; updates still need compatibility checks and review |
| Repository metadata | `qa-java-pact`; verb-led description under 110 characters; eight category/language/tool/technique topics | Metadata is documented below and verified against GitHub during publication |
| License | MIT, copyright 2026 Yashwant Das, Maven license metadata | Root `LICENSE`, `pom.xml`, README license link |
| Controlled verification | Actual client/provider, file-based WireMock, numeric-price break/fix, missing-contract failure, 28 script guard cases | 16 tests/invocations pass; Docker and standalone-JAR smoke checks passed |
| External boundaries | Opt-in PactFlow and manual Mockoon exercise | Live account publishing and desktop import are not certified by local or hosted checks |

### Publication metadata

Repository: [yashwant-das/qa-java-pact](https://github.com/yashwant-das/qa-java-pact), public, default branch `main`.

Description: “Tests API compatibility with Pact-JVM, WireMock and PactFlow”.

Topics: `test-automation`, `java`, `pact`, `wiremock`, `pactflow`, `junit`, `api-testing`, `contract-testing`.

The owner explicitly requested repository creation, metadata updates and pushing on 2026-11-07. This authorises GitHub publication; it does not authorise publishing Pact contracts or verification results to a PactFlow account.

The `Java contracts and quality` workflow check must pass before enabling it as a required check. Hosted execution and enforcement evidence are recorded after publication. Java CodeQL scanning is configured for pushes, pull requests and a weekly schedule; it has only the permissions needed to upload security results and is separate from PMD. Owner for pending hosted/settings evidence: project maintainer. Review trigger: first publication and any CI/security configuration change.
