package org.pmiops.workbench.consumer;

import static io.pactfoundation.consumer.dsl.LambdaDsl.newJsonBody;
import static org.junit.jupiter.api.Assertions.assertEquals;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import java.util.ArrayList;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.pmiops.workbench.leonardo.ApiClient;
import org.pmiops.workbench.leonardo.ApiException;
import org.pmiops.workbench.leonardo.api.AppsApi;
import org.pmiops.workbench.leonardo.model.LeonardoAppStatus;
import org.pmiops.workbench.leonardo.model.LeonardoAppType;
import org.pmiops.workbench.leonardo.model.LeonardoCloudContext;
import org.pmiops.workbench.leonardo.model.LeonardoGetAppResponse;

@ExtendWith(PactConsumerTestExt.class)
class AppsApiTest {

  @Pact(consumer = "aou-rwb-api", provider = "leonardo")
  RequestResponsePact getApp(PactDslWithProvider builder) {
    return builder
        .given("there is an app in a Google project")
        .uponReceiving("a request to get that app")
        .method("GET")
        .path("/api/google/v1/apps/googleProject/appname")
        .willRespondWith()
        .status(200)
        .headers(contentTypeJsonHeader)
        .body(
            newJsonBody(
                    body -> {
                      body.stringType("appName", "sample-cromwell-study");
                      body.stringType("status", "RUNNING");
                      body.stringType("diskName", "disk-123");
                      body.stringType("appType", "CROMWELL");
                      body.array("errors", errors -> {});
                      body.object(
                          "cloudContext", context -> context.stringType("cloudprovider", null));
                    })
                .build())
        .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "getApp")
  void testGetAppWhenAppExists(MockServer mockServer) throws ApiException {
    ApiClient client = new ApiClient();
    client.setBasePath(mockServer.getUrl());
    AppsApi leoAppService = new AppsApi(client);

    LeonardoGetAppResponse expected = new LeonardoGetAppResponse();
    expected.setAppName("sample-cromwell-study");
    expected.setErrors(new ArrayList<>());
    expected.setDiskName("disk-123");
    expected.setStatus(LeonardoAppStatus.RUNNING);
    expected.setAppType(LeonardoAppType.CROMWELL);
    expected.setCloudContext(new LeonardoCloudContext());

    LeonardoGetAppResponse response = leoAppService.getApp("googleProject", "appname");

    assertEquals(expected, response);
  }

  static Map<String, String> contentTypeJsonHeader = Map.of("Content-Type", "application/json");
}
