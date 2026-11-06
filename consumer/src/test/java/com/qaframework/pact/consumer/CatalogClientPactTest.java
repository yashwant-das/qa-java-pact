package com.qaframework.pact.consumer;

import static org.junit.jupiter.api.Assertions.*;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTest;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Map;
import org.junit.jupiter.api.Test;

@PactConsumerTest
@PactTestFor(providerName = "CatalogApi", pactVersion = PactSpecVersion.V3)
class CatalogClientPactTest {
  @Pact(consumer = "StorefrontClient")
  RequestResponsePact firstPage(PactDslWithProvider builder) {
    var body =
        new PactDslJsonBody()
            .integerType("page", 1)
            .booleanType("hasNext", true)
            .minArrayLike("products", 1)
            .stringType("id", "sku-123")
            .stringType("name", "Daily moisturiser")
            .numberType("price", 19990.00)
            .stringValue("currency", "CLP")
            .closeObject()
            .closeArray();
    return builder
        .given("skincare products exist")
        .uponReceiving("the first skincare page")
        .path("/products")
        .method("GET")
        .query("category=skincare&page=1")
        .willRespondWith()
        .status(200)
        .headers(Map.of("Content-Type", "application/json"))
        .body(body)
        .toPact();
  }

  @Pact(consumer = "StorefrontClient")
  RequestResponsePact exhaustedPage(PactDslWithProvider builder) {
    return builder
        .given("skincare products exist")
        .uponReceiving("a page after the last skincare product")
        .path("/products")
        .method("GET")
        .query("category=skincare&page=3")
        .willRespondWith()
        .status(200)
        .headers(Map.of("Content-Type", "application/json"))
        .body(
            new PactDslJsonBody()
                .integerType("page", 3)
                .booleanValue("hasNext", false)
                .array("products")
                .closeArray())
        .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "firstPage")
  void readsProductsThroughTheRealClient(MockServer server) throws Exception {
    var page = new CatalogClient(URI.create(server.getUrl())).search("skincare", 1);
    assertEquals(1, page.page());
    assertTrue(page.hasNext());
    assertEquals("sku-123", page.products().getFirst().id());
    assertEquals("Daily moisturiser", page.products().getFirst().name());
    assertEquals(0, new BigDecimal("19990.00").compareTo(page.products().getFirst().price()));
    assertEquals("CLP", page.products().getFirst().currency());
  }

  @Test
  @PactTestFor(pactMethod = "exhaustedPage")
  void readsAnExhaustedPage(MockServer server) throws Exception {
    var page = new CatalogClient(URI.create(server.getUrl())).search("skincare", 3);
    assertEquals(3, page.page());
    assertTrue(page.products().isEmpty());
    assertFalse(page.hasNext());
  }
}
