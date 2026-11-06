package com.qaframework.pact.provider;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.PactBrokerAuth;
import au.com.dius.pact.provider.junitsupport.loader.PactBrokerConsumerVersionSelectors;
import au.com.dius.pact.provider.junitsupport.loader.SelectorBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

@Provider("CatalogApi")
@PactBroker(
    url = "${PACT_BROKER_BASE_URL}",
    authentication = @PactBrokerAuth(token = "${PACT_BROKER_TOKEN}"))
class BrokerVerificationTest {
  private CatalogServer server;

  @PactBrokerConsumerVersionSelectors
  public static SelectorBuilder consumers() {
    return new SelectorBuilder().mainBranch().deployedOrReleased();
  }

  @BeforeEach
  void start(PactVerificationContext context) throws Exception {
    server = new CatalogServer(0, System.getProperty("catalog.priceShape", "number"));
    server.start();
    context.setTarget(new HttpTestTarget("127.0.0.1", server.port()));
  }

  @State("skincare products exist")
  void productsExist() {
    // The immutable sample catalog starts with this state for every interaction.
  }

  @TestTemplate
  @ExtendWith(PactVerificationInvocationContextProvider.class)
  void verifiesPublishedConsumerContracts(PactVerificationContext context) {
    context.verifyInteraction();
  }

  @AfterEach
  void stop() {
    if (server != null) server.close();
  }
}
