package com.qaframework.pact.provider;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CatalogServerTest {
  private CatalogServer server;
  private final HttpClient client = HttpClient.newHttpClient();
  private final ObjectMapper mapper = new ObjectMapper();

  @BeforeEach
  void start() throws Exception {
    server = new CatalogServer(0, "number");
    server.start();
  }

  @AfterEach
  void stop() {
    server.close();
  }

  private HttpResponse<String> get(String path) throws Exception {
    return client.send(
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.port() + path))
            .GET()
            .build(),
        HttpResponse.BodyHandlers.ofString());
  }

  @Test
  void returnsDistinctPagesAndTerminatesPagination() throws Exception {
    var first = mapper.readTree(get("/products?category=skincare&page=1").body());
    var second = mapper.readTree(get("/products?category=skincare&page=2").body());
    var last = mapper.readTree(get("/products?category=skincare&page=3").body());
    assertEquals("sku-123", first.at("/products/0/id").asText());
    assertEquals("sku-456", second.at("/products/0/id").asText());
    assertTrue(first.get("hasNext").asBoolean());
    assertFalse(second.get("hasNext").asBoolean());
    assertTrue(last.get("products").isEmpty());
  }

  @Test
  void returnsEmptyResultsForAnUnknownCategory() throws Exception {
    var response = get("/products?category=unknown&page=1");
    assertEquals(200, response.statusCode());
    assertTrue(mapper.readTree(response.body()).get("products").isEmpty());
  }

  @Test
  void rejectsInvalidPaginationAndQueryStrings() throws Exception {
    for (var query :
        new String[] {
          "category=skincare&page=0",
          "category=skincare&page=no",
          "category=skincare",
          "page=1",
          "category=x&page=1&page=2"
        }) {
      assertEquals(400, get("/products?" + query).statusCode(), query);
    }
  }

  @Test
  void unknownRoutesReturnJsonAndPostIsRejected() throws Exception {
    var missing = get("/unknown");
    assertEquals(404, missing.statusCode());
    assertTrue(mapper.readTree(missing.body()).has("error"));
    var request =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.port() + "/products"))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();
    assertEquals(405, client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode());
  }
}
