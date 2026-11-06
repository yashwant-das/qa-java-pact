# Catalog API and Postman guide

Use this guide to start a server, send requests manually and understand the responses. Import [the Postman collection](../postman/catalog.postman_collection.json) to get 18 requests with descriptions, saved response examples and response assertions.

## Choose a server

| Server | Default base URL | What it does | Postman folder |
| --- | --- | --- | --- |
| Real catalog provider | `http://127.0.0.1:8081` | Runs `CatalogServer` and implements search, pagination and validation | `Provider API` |
| WireMock standalone | `http://127.0.0.1:8080` | Serves explicitly mapped responses, including fault scenarios | `WireMock scenarios` |

Both servers are local and require no authentication. Requests use their folder's URL variable automatically. Start both servers only if you want to run the entire collection; otherwise run the relevant folder.

### Start the real provider

From the repository root, with JDK 21 selected:

```bash
./mvnw -pl consumer -am install -DskipTests
./mvnw -pl provider compile exec:java
```

The first command installs the parent and consumer artifacts needed by the provider module. The second keeps the API running in that terminal. Wait for the `Catalog API: http://127.0.0.1:8081/products?category=skincare&page=1` message, then use a separate terminal or Postman. Stop with Ctrl+C.

To use another port:

```bash
CATALOG_PORT=9081 ./mvnw -pl provider compile exec:java
```

Update the collection's `providerBaseUrl` to `http://127.0.0.1:9081` afterward.

### Start WireMock

From the repository root:

```bash
docker compose up -d
curl 'http://127.0.0.1:8080/products?category=skincare&page=1'
```

Wait for the request to return JSON before using Postman. Stop this server with `docker compose down`. WireMock does not start the real provider; the provider command does not start WireMock.

## Import and send a request in Postman

1. Open the Postman desktop app and choose **Import**.
2. Select `postman/catalog.postman_collection.json` from this repository. It uses the Postman Collection v2.1 format.
3. Open the imported **API Contract Testing · Local Catalog** collection. No separate environment file or token is required.
4. In the collection's **Variables**, inspect `providerBaseUrl` and `mockBaseUrl`. Their defaults are the two local URLs above. Change these if you selected different ports; use an origin without a trailing slash.
5. For the real API, open **Provider API → 01 · Products page 1**. The request is `GET {{providerBaseUrl}}/products?category=skincare&page=1`.
6. In **Params**, confirm `category=skincare` and `page=1`, then choose **Send**. The request has no body. It sends `Accept: application/json` and uses **No Auth**.
7. Inspect the HTTP status, response body and the response test results. The first request should return 200 and product `sku-123`, with all assertions passing.
8. Try page 2, page 3, validation failures or the WireMock folder. Each request describes its expected result. Saved response examples let you inspect expected payloads before running a server.

The web version of Postman needs a local-capable agent, such as the Desktop Agent, to reach these loopback servers. A remote/cloud agent cannot reach a server running on your laptop. Environment or global variables with the same names can override collection variables; check those if a request goes to an unexpected URL.

For a manual request without importing, create a GET request to `http://127.0.0.1:8081/products`, add the two Params rows above and choose **Send**.

## Real provider reference

### Search products

`GET /products`

| Query parameter | Required | Behaviour |
| --- | --- | --- |
| `category` | Yes | Nonblank string. The sample contains exactly `skincare`; other values return an empty page. Matching is case-sensitive. |
| `page` | Yes | Positive 32-bit integer, starting at 1. There is no default page. |

Duplicate query parameter keys are rejected. Unique extra parameters are ignored by this sample. Use Postman's Params editor to encode spaces or special characters in values.

Example request:

```bash
curl -i 'http://127.0.0.1:8081/products?category=skincare&page=1'
```

Example 200 response:

```json
{
  "page": 1,
  "hasNext": true,
  "products": [
    {
      "id": "sku-123",
      "name": "Daily moisturiser",
      "price": 19990,
      "currency": "CLP"
    }
  ]
}
```

| Field | Type | Meaning |
| --- | --- | --- |
| `page` | Integer | Requested page number |
| `hasNext` | Boolean | Whether another product page follows |
| `products` | Array | Zero or one products in this sample |
| `products[].id` | String | Product identifier |
| `products[].name` | String | Product display name |
| `products[].price` | Number | Price amount; consumers accept integer or decimal JSON numbers |
| `products[].currency` | String | `CLP` in this example |

The provider sends JSON with `Content-Type: application/json; charset=utf-8` and `Cache-Control: no-store`.

### Pagination and validation examples

| Request | Status | Expected result |
| --- | --- | --- |
| `GET /products?category=skincare&page=1` | 200 | `sku-123`, price 19990, `hasNext=true` |
| `GET /products?category=skincare&page=2` | 200 | `sku-456`, price 24990, `hasNext=false` |
| `GET /products?category=skincare&page=3` | 200 | Empty products array, `hasNext=false` |
| `GET /products?category=skincare&page=4` | 200 | Also empty; later valid pages remain empty |
| `GET /products?category=unknown&page=1` | 200 | Empty products array, `hasNext=false` |
| `GET /products?category=skincare&page=0` | 400 | Page must be positive |
| `GET /products?category=skincare&page=abc` | 400 | Page must be an integer |
| `GET /products?page=1` | 400 | Missing category |
| `GET /products?category=skincare` | 400 | Missing page |
| `GET /products?category=skincare&page=1&page=2` | 400 | Duplicate page parameter |
| `POST /products?category=skincare&page=1` | 405 | Unsupported method; `Allow: GET` |
| `GET /unknown` | 404 | Unknown route |

Validation errors use:

```json
{
  "error": "Provide category and a positive integer page"
}
```

The 405 response is `{"error":"Method not allowed"}`. An unknown route returns `{"error":"Route not found"}`. The API has no authentication, product-detail, create/update/delete, cart or order endpoints.

## WireMock scenario reference

Send these requests to port **8080**, not the real provider's port. They are explicit fixtures, not additional provider features.

| Request | Status | Expected result |
| --- | --- | --- |
| `GET /products?category=skincare&page=1` | 200 | First product, `hasNext=true` |
| `GET /products?category=skincare&page=2` | 200 | Second product, `hasNext=false` |
| `GET /products?category=skincare&page=3` | 200 | Empty products array |
| `GET /products?category=unavailable&page=1` | 503 | `{"error":"Catalog temporarily unavailable"}` |
| `GET /products?category=slow&page=1` | 200 | First-page body after a configured 750 ms delay |
| `GET /products?category=incompatible&page=1` | 200 | Product price is an object containing `amount` and `currency` |
| `GET /products?category=unknown&page=1` | 404 | `{"error":"No matching mock scenario"}` |

WireMock has no mapping for page 4 or a general validation layer. Unmatched requests, including unsupported methods and invalid queries, return the 404 fixture; they do not inherit the real provider's 400/405 behaviour. Similarly, `category=unavailable` on the real provider returns an empty 200 page, not a service outage.

The incompatible-price fixture deliberately violates the consumer's numeric-price expectation:

```json
{
  "amount": 19990,
  "currency": "CLP"
}
```

Its Postman test checks that the intended fault fixture was returned. A green Postman test for this request does not certify consumer compatibility. The Pact provider verification exercise is documented in the [learning path](learning-path.md#3-observe-a-real-contract-failure).

## Run the collection

With one server running, use Postman's Collection Runner on its matching folder. With both servers running, run the whole collection. Keep the request timeout above the slow fixture's 750 ms delay; 5000 ms is a useful local setting. HTTP 400/404/405/503 cases pass when those expected statuses and bodies are returned.

For a local command-line run, Newman is optional. With Node 24 and both servers running:

```bash
npx --yes newman@6.2.3 run postman/catalog.postman_collection.json --timeout-request 5000
```

Run only the provider folder:

```bash
npx --yes newman@6.2.3 run postman/catalog.postman_collection.json --folder 'Provider API' --timeout-request 5000
```

For different local ports, pass `--env-var providerBaseUrl=http://127.0.0.1:9081` or `--env-var mockBaseUrl=http://127.0.0.1:9080`. These names intentionally match the collection variables. Newman executes requests locally; this command does not require a Postman account or publish a collection.

Postman assertions complement the Java tests. They exercise sample requests and fixtures; they do not publish Pact contracts or verify a real API against a Pact contract. The standard Maven CI job checks the collection's JSON syntax through pre-commit; running the collection requires the servers described above.

## Troubleshooting

- **Connection refused:** start the relevant server and confirm the port. A passing Maven test run starts temporary servers and then stops them; it does not leave either manual server running.
- **Unexpected 404 or 200:** check which server the request targets. The real provider and WireMock intentionally behave differently for unknown categories, methods and unmapped pages.
- **Slow request times out:** increase the request timeout above 750 ms. To deliberately explore timeout behaviour, temporarily choose a lower timeout and expect a network error rather than a completed response assertion.
- **Unresolved `{{...}}` variable:** import the entire collection, then check its Variables tab and any environment overrides.
- **Full collection fails on one folder:** both servers must be running for a full run. Choose one folder if only one server is available.

See the official [collection import guide](https://learning.postman.com/docs/getting-started/importing-and-exporting/importing-data/), [Postman scripts guide](https://learning.postman.com/docs/tests-and-scripts/write-scripts/intro-to-scripts/) and [collection schema](https://schema.postman.com/collection/json/v2.1.0/draft-07/docs/index.html) for Postman-specific details.
