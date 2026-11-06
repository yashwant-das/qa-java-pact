package com.qaframework.pact.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** HTTP client exercised by the consumer contract and robustness tests. */
public final class CatalogClient {
  private final URI origin;
  private final Duration timeout;
  private final HttpClient http;
  private final ObjectMapper mapper = new ObjectMapper();

  public CatalogClient(URI origin) {
    this(origin, Duration.ofSeconds(2));
  }

  public CatalogClient(URI origin, Duration timeout) {
    if (origin == null
        || !("http".equals(origin.getScheme()) || "https".equals(origin.getScheme()))
        || origin.getHost() == null
        || origin.getRawQuery() != null
        || origin.getFragment() != null
        || origin.getUserInfo() != null
        || !(origin.getPath().isEmpty() || "/".equals(origin.getPath()))) {
      throw new IllegalArgumentException(
          "Use an HTTP(S) origin without a path, credentials, query or fragment");
    }
    if (timeout == null || timeout.isNegative() || timeout.isZero()) {
      throw new IllegalArgumentException("Timeout must be positive");
    }
    this.origin = origin;
    this.timeout = timeout;
    this.http = HttpClient.newBuilder().connectTimeout(timeout).build();
  }

  public ProductPage search(String category, int page) throws IOException, InterruptedException {
    if (category == null || category.isBlank() || page < 1) {
      throw new IllegalArgumentException("Category must be nonblank and page must be positive");
    }
    var query = "category=" + URLEncoder.encode(category, StandardCharsets.UTF_8) + "&page=" + page;
    var request =
        HttpRequest.newBuilder(origin.resolve("/products?" + query))
            .timeout(timeout)
            .header("Accept", "application/json")
            .GET()
            .build();
    var response = http.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() != 200) throw new ApiException(response.statusCode());
    return parse(response.body());
  }

  private ProductPage parse(String body) throws IOException {
    var root = mapper.readTree(body);
    if (root == null
        || !root.path("page").isIntegralNumber()
        || !root.path("page").canConvertToInt()
        || root.path("page").intValue() < 1
        || !root.path("hasNext").isBoolean()
        || !root.path("products").isArray()) {
      throw new IOException("Invalid catalog page");
    }
    var products = new ArrayList<Product>();
    for (JsonNode product : root.get("products")) {
      if (!product.path("id").isTextual()
          || !product.path("name").isTextual()
          || !product.path("price").isNumber()
          || !product.path("currency").isTextual()) {
        throw new IOException("Invalid product: id, name, numeric price and currency are required");
      }
      products.add(
          new Product(
              product.get("id").textValue(),
              product.get("name").textValue(),
              product.get("price").decimalValue(),
              product.get("currency").textValue()));
    }
    return new ProductPage(
        root.get("page").intValue(), root.get("hasNext").booleanValue(), List.copyOf(products));
  }

  public record Product(String id, String name, BigDecimal price, String currency) {}

  public record ProductPage(int page, boolean hasNext, List<Product> products) {}

  public static final class ApiException extends IOException {
    private final int statusCode;

    ApiException(int statusCode) {
      super("Catalog API returned HTTP " + statusCode);
      this.statusCode = statusCode;
    }

    public int statusCode() {
      return statusCode;
    }
  }
}
