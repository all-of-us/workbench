package org.pmiops.workbench.consumer;

import static io.pactfoundation.consumer.dsl.LambdaDsl.newJsonBody;
import static org.junit.jupiter.api.Assertions.assertEquals;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.pmiops.workbench.leonardo.ApiClient;
import org.pmiops.workbench.leonardo.ApiException;
import org.pmiops.workbench.leonardo.api.AppsApi;
import org.pmiops.workbench.leonardo.model.LeonardoGetAppResponse;

@ExtendWith(PactConsumerTestExt.class)
class ProductServiceTest {

  @Pact(consumer = "x", provider = "y")
  RequestResponsePact getApp(PactDslWithProvider builder) throws ApiException {
    return builder
        .given("a")
        .uponReceiving("b")
        .method("GET")
        .path("/api/google/v1/apps/googleProject/appName")
        .willRespondWith()
        .status(200)
        .headers(headers())
        .body(newJsonBody(body -> {
          body.stringType("appName", "MinivanB");
          body.stringType("status", "RUNNING");
          body.stringType("diskName", "Porg");
          body.stringType("appType","CROMWELL");
          body.array("errors", errors -> {});
        }).build())
        .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "getApp")
  void getProductById_whenProductWithId10Exists(MockServer mockServer) throws ApiException {
    ApiClient client = new ApiClient();
    client.setBasePath(mockServer.getUrl());
    AppsApi leoAppService = new AppsApi(client);
    LeonardoGetAppResponse expected = new LeonardoGetAppResponse();
    expected.setAppName("Minivan");

    LeonardoGetAppResponse response = leoAppService.getApp("googleProject", "appName");

    assertEquals(expected, response);
  }

  private Map<String, String> headers() {
    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json; charset=utf-8");
    return headers;
  }
}
