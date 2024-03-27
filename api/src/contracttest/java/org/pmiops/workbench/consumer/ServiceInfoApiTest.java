package org.pmiops.workbench.consumer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.pmiops.workbench.leonardo.ApiClient;
import org.pmiops.workbench.leonardo.ApiException;
import org.pmiops.workbench.leonardo.api.ServiceInfoApi;

@ExtendWith(PactConsumerTestExt.class)
class ServiceInfoApiTest {

  private static final String STATUS_ENDPOINT = "/status";
  private ServiceInfoApi api;

  @BeforeEach
  void setUp(MockServer mockServer) {
    ApiClient client = new ApiClient();
    client.setBasePath(mockServer.getUrl());
    api = new ServiceInfoApi(client);
  }

  @Pact(consumer = "aou-rwb-api", provider = "leonardo")
  RequestResponsePact getStatusOk(PactDslWithProvider builder) {
    return builder
        .given("the system is ok")
        .uponReceiving("a request for the system's status")
        .method("GET")
        .path(STATUS_ENDPOINT)
        .willRespondWith()
        .status(200)
        .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "getStatusOk")
  void testGetSystemStatusWhenSystemOk() {
    assertDoesNotThrow(api::getSystemStatus);
  }

  @Pact(consumer = "aou-rwb-api", provider = "leonardo")
  RequestResponsePact getStatusDown(PactDslWithProvider builder) {
    return builder
        .given("the system is down")
        .uponReceiving("a request for the system's status")
        .method("GET")
        .path(STATUS_ENDPOINT)
        .willRespondWith()
        .status(500)
        .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "getStatusDown")
  void testGetSystemStatusWhenSystemDown() {
    assertThrows(ApiException.class, () -> api.getSystemStatus());
  }
}
