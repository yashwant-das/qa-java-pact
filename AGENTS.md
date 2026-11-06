# Agent instructions

## Scope and standards

This is a Java 21 / Maven contract-testing learning project. Its documentation profile is `complete`. The README uses the descriptive title “API Contract Testing with Pact”; keep repository identifiers in paths and metadata.
Follow [repo-standards at a429dd2f2da55a4c3e1c73c7c0d1111f9cd0b9a9](https://github.com/yashwant-das/repo-standards/tree/a429dd2f2da55a4c3e1c73c7c0d1111f9cd0b9a9).
The namespace is `com.qaframework.pact`; the repository name is `qa-java-pact`.

## Working rules

- Run `./mvnw clean verify` and the script guard tests after relevant changes.
- Keep provider models independent from consumer models. The provider's test dependency on the consumer establishes reactor order only.
- Generate Pact contracts by testing `CatalogClient`; never hand-edit generated contracts.
- Verify the real `CatalogServer`, not WireMock or Mockoon.
- Keep cloud operations explicit. Default checks must work without PactFlow credentials.
- Never publish contracts, results or releases without an explicit request to perform that operation.
- Use fictional fixtures. Do not copy client/company payloads or secrets into this repository.
- No empty-test success: missing local or broker contracts must fail verification.
- Run pre-commit before committing. Do not bypass hooks.

## Documentation

README is the entry point. `docs/development.md` owns commands and configuration; `docs/architecture.md` owns boundaries and trade-offs; `docs/learning-path.md` owns exercises; `docs/api.md` owns the API reference and Postman workflow. The importable collection is `postman/catalog.postman_collection.json`; keep examples and assertions aligned with the real provider and mock fixtures. `docs/superpowers/` contains historical planning records.
