package com.qaframework.pact.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Fictional catalog provider. No consumer types or mock server are used here. */
public final class CatalogServer implements AutoCloseable {
  private static final List<Map<String, Object>> PRODUCTS =
      List.of(
          Map.of("id", "sku-123", "name", "Daily moisturiser", "price", 19990, "currency", "CLP"),
          Map.of("id", "sku-456", "name", "Mineral sunscreen", "price", 24990, "currency", "CLP"));
  private final HttpServer server;
  private final ObjectMapper mapper = new ObjectMapper();
  private final String priceShape;

  // Bind only to IPv4 loopback so local exercises never expose the provider on the LAN.
  @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
  public CatalogServer(int port, String priceShape) throws IOException {
    if (!("number".equals(priceShape) || "object".equals(priceShape))) {
      throw new IllegalArgumentException("Price shape must be number or object");
    }
    this.priceShape = priceShape;
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
    server.createContext("/", this::handle);
  }

  public void start() {
    server.start();
  }

  public int port() {
    return server.getAddress().getPort();
  }

  private void handle(HttpExchange exchange) throws IOException {
    try (exchange) {
      if (!"/products".equals(exchange.getRequestURI().getPath())) {
        respond(exchange, 404, Map.of("error", "Route not found"));
      } else if (!"GET".equals(exchange.getRequestMethod())) {
        exchange.getResponseHeaders().set("Allow", "GET");
        respond(exchange, 405, Map.of("error", "Method not allowed"));
      } else {
        Map<String, String> query;
        int page;
        try {
          query = parseQuery(exchange.getRequestURI().getRawQuery());
          page = Integer.parseInt(query.getOrDefault("page", ""));
          if (page < 1 || query.getOrDefault("category", "").isBlank()) {
            throw new IllegalArgumentException("Invalid query");
          }
        } catch (IllegalArgumentException exception) {
          respond(exchange, 400, Map.of("error", "Provide category and a positive integer page"));
          return;
        }
        boolean known = "skincare".equals(query.get("category"));
        List<Map<String, Object>> products = List.of();
        if (known && page <= PRODUCTS.size()) {
          var product = new HashMap<>(PRODUCTS.get(page - 1));
          if ("object".equals(priceShape)) {
            product.put("price", Map.of("amount", product.get("price"), "currency", "CLP"));
          }
          products = List.of(product);
        }
        respond(
            exchange,
            200,
            Map.of("page", page, "hasNext", known && page < PRODUCTS.size(), "products", products));
      }
    }
  }

  private static Map<String, String> parseQuery(String raw) {
    var query = new HashMap<String, String>();
    if (raw != null) {
      for (var pair : raw.split("&")) {
        var parts = pair.split("=", 2);
        var key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
        var value = parts.length == 2 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
        if (query.putIfAbsent(key, value) != null) {
          throw new IllegalArgumentException("Duplicate query parameter");
        }
      }
    }
    return query;
  }

  private void respond(HttpExchange exchange, int status, Map<String, ?> body) throws IOException {
    byte[] bytes = mapper.writeValueAsBytes(body);
    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
    exchange.getResponseHeaders().set("Cache-Control", "no-store");
    exchange.sendResponseHeaders(status, bytes.length);
    exchange.getResponseBody().write(bytes);
  }

  @Override
  public void close() {
    server.stop(0);
  }

  public static void main(String[] args) throws IOException {
    int port = Integer.parseInt(System.getenv().getOrDefault("CATALOG_PORT", "8081"));
    var server = new CatalogServer(port, "number");
    Runtime.getRuntime().addShutdownHook(new Thread(server::close));
    server.start();
    System.out.println(
        "Catalog API: http://127.0.0.1:" + server.port() + "/products?category=skincare&page=1");
  }
}
