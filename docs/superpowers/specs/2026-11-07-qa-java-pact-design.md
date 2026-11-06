# qa-java-pact design record

Historical record of the scope selected in chat on 2026-11-07. Current setup lives in the README and development guide.

## Intent

Teach a Java-experienced API tester consumer-driven contracts, provider verification and PactFlow release compatibility through a runnable ecommerce example. Create a fresh local repository under `~/Workspace/GitHub/qa-java-pact`; leave the old Falabella project untouched.

## Approach

Java 21 and Maven were selected over TypeScript because the owner knows Java and their standards use Java 21. Two Maven modules keep independent consumer and provider models. The consumer exercises its real HTTP client against Pact; the provider verifies the generated contract against a real loopback HTTP server. WireMock handles fault and pagination exercises. Mockoon is an optional comparison, not a second required mock runtime. PactFlow publishing is explicit and separate from the credential-free default build.

## Boundaries

Use fictional products and no Falabella code or payloads. No UI, database or deployment is needed. Never report mock-against-mock verification as provider compatibility. Keep malformed payloads and outages in client robustness tests rather than publishing them as provider promises. Keep credentials in environment variables and out of generated artifacts. Do not create a GitHub remote or publish anything as part of local setup.

## Observable result

`./mvnw clean verify` generates a consumer contract then verifies both interactions against the sample provider. Changing the provider price from a number to an object fails contract verification. A separate broker profile fetches contracts and can publish results when explicitly enabled. Documentation explains supported mobile releases, branches, `can-i-deploy`, and the limits of each test layer.
