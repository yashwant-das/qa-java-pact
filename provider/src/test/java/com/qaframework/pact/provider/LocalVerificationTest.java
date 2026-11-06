package com.qaframework.pact.provider;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

@Provider("CatalogApi")
@PactFolder("../consumer/target/pacts")
class LocalVerificationTest {
  private CatalogServer server;

  @BeforeEach
  void start(PactVerificationContext context) throws Exception {
    server = new CatalogServer(0, System.getProperty("catalog.priceShape", "number"));
    server.start();
    context.setTarget(new HttpTestTarget("127.0.0.1", server.port()));
  }

  @State("skincare products exist")
  void productsExist() {
    // The sample catalog is immutable and starts with this provider state.
  }

  @TestTemplate
  @ExtendWith(PactVerificationInvocationContextProvider.class)
  void verifiesTheRealProvider(PactVerificationContext context) {
    context.verifyInteraction();
  }

  @AfterEach
  void stop() {
    if (server != null) server.close();
  }
}
