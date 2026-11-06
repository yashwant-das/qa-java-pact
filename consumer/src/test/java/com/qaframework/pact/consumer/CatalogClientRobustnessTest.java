package com.qaframework.pact.consumer;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CatalogClientRobustnessTest {
  private WireMockServer server;
  private CatalogClient client;

  @BeforeEach
  void start() {
    server =
        new WireMockServer(
            WireMockConfiguration.wireMockConfig()
                .dynamicPort()
                .usingFilesUnderDirectory("../wiremock"));
    server.start();
    client = new CatalogClient(URI.create(server.baseUrl()));
  }

  @AfterEach
  void stop() {
    server.stop();
  }

  @Test
  void paginationDoesNotRepeatTheFirstProduct() throws Exception {
    var first = client.search("skincare", 1);
    var second = client.search("skincare", 2);
    var last = client.search("skincare", 3);
    assertEquals("sku-123", first.products().getFirst().id());
    assertEquals("sku-456", second.products().getFirst().id());
    assertTrue(first.hasNext());
    assertFalse(second.hasNext());
    assertTrue(last.products().isEmpty());
    assertFalse(last.hasNext());
  }

  @Test
  void encodesCategoryAsOneQueryValue() throws Exception {
    server.stubFor(
        get(urlPathEqualTo("/products"))
            .withQueryParam("category", equalTo("skin care & sun"))
            .withQueryParam("page", equalTo("1"))
            .willReturn(okJson("{\"page\":1,\"hasNext\":false,\"products\":[]}")));
    assertTrue(client.search("skin care & sun", 1).products().isEmpty());
  }

  @Test
  void exposesHttpFailureStatus() {
    var failure =
        assertThrows(CatalogClient.ApiException.class, () -> client.search("unavailable", 1));
    assertEquals(503, failure.statusCode());
  }

  @Test
  void timesOutWhenTheServiceIsSlow() {
    var impatient = new CatalogClient(URI.create(server.baseUrl()), Duration.ofMillis(150));
    assertThrows(HttpTimeoutException.class, () -> impatient.search("slow", 1));
  }

  @Test
  void rejectsTheBreakingPriceShape() {
    assertThrows(IOException.class, () -> client.search("incompatible", 1));
  }

  @Test
  void rejectsMissingRequiredFields() {
    server.stubFor(
        get(urlPathEqualTo("/products"))
            .withQueryParam("category", equalTo("missing"))
            .willReturn(
                okJson("{\"page\":1,\"hasNext\":false,\"products\":[{\"id\":\"sku-123\"}]}")));
    assertThrows(IOException.class, () -> client.search("missing", 1));
  }

  @Test
  void acceptsAdditionalProviderFields() throws Exception {
    server.stubFor(
        get(urlPathEqualTo("/products"))
            .withQueryParam("category", equalTo("extended"))
            .willReturn(
                okJson(
                    "{\"page\":1,\"hasNext\":false,\"traceId\":\"extra\",\"products\":[{\"id\":\"sku-123\",\"name\":\"Cream\",\"price\":19990,\"currency\":\"CLP\",\"brand\":\"Example\"}]}")));
    assertEquals("sku-123", client.search("extended", 1).products().getFirst().id());
  }

  @Test
  void rejectsInvalidArgumentsBeforeSendingARequest() {
    assertThrows(IllegalArgumentException.class, () -> client.search("skincare", 0));
    assertThrows(IllegalArgumentException.class, () -> client.search(" ", 1));
    assertThrows(
        IllegalArgumentException.class, () -> new CatalogClient(URI.create("file:///tmp/catalog")));
  }
}
