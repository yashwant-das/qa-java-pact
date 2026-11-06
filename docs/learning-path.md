# Learning path

Work from the repository root. Start with [development setup](development.md), then follow these exercises in order.

## 1. Generate a contract from the actual client

Run `./mvnw clean verify`. Open `consumer/src/test/java/com/qaframework/pact/consumer/CatalogClientPactTest.java` alongside `consumer/target/pacts/StorefrontClient-CatalogApi.json`.

Find the consumer/provider names, provider state, query parameters and response matchers. Notice that the test calls `CatalogClient.search`, not a generic HTTP request embedded in the test. The contract therefore captures a boundary exercised by real consumer code.

The first-page interaction needs product identity, name, numeric price and currency. The exhausted-page interaction needs an empty products array and `hasNext=false`. Provider verification starts the real sample API and checks both requests independently.

For manual requests, import the collection and follow the [API and Postman guide](api.md). Its folders keep real provider behaviour separate from WireMock fault scenarios.

## 2. Explore file-based mocks

Start `docker compose up -d`. Request successive pages:

```bash
curl 'http://localhost:8080/products?category=skincare&page=1'
curl 'http://localhost:8080/products?category=skincare&page=2'
curl 'http://localhost:8080/products?category=skincare&page=3'
```

Expect different products on the first two pages and an empty third page. Inspect the matching JSON in `wiremock/mappings/` and the bodies in `wiremock/__files/`.

Then try fault scenarios:

```bash
curl -i 'http://localhost:8080/products?category=unavailable&page=1'
curl -i 'http://localhost:8080/products?category=slow&page=1'
curl -i 'http://localhost:8080/products?category=incompatible&page=1'
```

Expect HTTP 503, a response delayed by 750 ms, and an object-shaped price respectively. `CatalogClientRobustnessTest` proves the actual client handles those inputs by exposing failures or rejecting the payload. The mock does not prove the real API behaves correctly.

Stop the container with `docker compose down`.

## 3. Observe a real contract failure

Run:

```bash
./mvnw test -Dcatalog.priceShape=object
```

Expect **BUILD FAILURE** in `LocalVerificationTest`: the first-page response contains an object where the contract requires a numeric price. The consumer tests still pass against their unchanged expectation, while the provider verification reveals the incompatibility.

For an automated demonstration that requires this specific mismatch, run `bash scripts/check-breaking-change.sh`. The script succeeds only when provider verification fails for the expected numeric-price incompatibility.

Restore the normal run with `./mvnw clean verify`. The switch affects only the test JVM, so no source restoration is needed. In a real system, a compatible migration could introduce an additional `priceDetails` field while retaining the old numeric `price` until supported clients have migrated.

## 4. Compare Mockoon's authoring workflow

Install Mockoon separately. In its environment menu choose the OpenAPI import option and open `mockoon/catalog.openapi.json`. Use port 8082 to avoid the other examples.

The specification supplies the product schema, success examples for three pages, and a service-error example. Inspect the imported responses; configure response rules matching query `page` to select the relevant success example, and a rule matching `category=unavailable` for HTTP 503. Make `skincare` page 1 your default response. Then compare the curl requests above using port 8082.

OpenAPI import is a starting point. Mockoon-specific response rules do not round-trip through OpenAPI; save the completed exercise as a native Mockoon data file outside generated build output if you want to reuse it. See [Mockoon's OpenAPI compatibility notes](https://mockoon.com/docs/latest/openapi/openapi-specification-compatibility/).

The deliverable here is a comparison exercise, not a preconfigured second mock system or a Mockoon-to-PactFlow adapter. Keep one authoritative mock implementation for each real use case.

## 5. Publish and verify with PactFlow

Follow [PactFlow setup](development.md#pactflow-setup). Publish the consumer contract under an immutable tested version and its branch. Configure the consumer main branch, then run broker provider verification and inspect both interaction results in PactFlow.

For the first learning run, check the exact consumer/provider version pair. Create a `learning` environment and record the sample provider deployment and consumer release there. These are simulated lifecycle events for the exercise.

Make a new local commit with an incompatible provider change, keep the existing consumer contract, publish the new provider verification result and run `can-i-deploy` for the provider version targeting `learning`. It should reject incompatibility with the supported consumer. Do not publish the teaching-switch result using the same immutable version as the numeric provider; different code/behaviour must have a different version.

For a mobile scenario, record two supported consumer releases with different expectations, then verify a new provider against both. Ending support for an older release is a product/support decision, not a test-failure workaround.

## 6. Understand the limits

Pact answers whether exercised consumer expectations and provider responses are compatible. Functional API tests answer whether the API's business rules work. WireMock and Mockoon make controlled situations reproducible. PactFlow shares versioned evidence and uses it for release checks.

As a next exercise, add a new provider field. Verify that compatibility remains green. Then remove a required client field and observe failure. Write the expectation in the real client test before updating the provider. Follow [Pact's consumer-test guidance](https://docs.pact.io/consumer) when deciding what belongs in the contract.
