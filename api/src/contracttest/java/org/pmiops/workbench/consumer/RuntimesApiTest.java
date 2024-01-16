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
import org.pmiops.workbench.leonardo.api.RuntimesApi;
import org.pmiops.workbench.leonardo.model.LeonardoCloudContext;
import org.pmiops.workbench.leonardo.model.LeonardoGetRuntimeResponse;
import org.pmiops.workbench.leonardo.model.LeonardoRuntimeStatus;

@ExtendWith(PactConsumerTestExt.class)
class RuntimesApiTest {

  @Pact(consumer = "aou-rwb-api", provider = "leonardo")
  RequestResponsePact getRuntime(PactDslWithProvider builder) {
    return builder
        .given("there is a runtime in a Google project")
        .uponReceiving("a request to get that runtime")
        .method("GET")
        .path("/api/google/v1/apps/googleProject/runtimeName")
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
  @PactTestFor(pactMethod = "getRuntime")
  void testGetRuntimeWhenRuntimeExists(MockServer mockServer) throws ApiException {
    ApiClient client = new ApiClient();
    client.setBasePath(mockServer.getUrl());
    RuntimesApi leoRuntimeService = new RuntimesApi(client);

    LeonardoGetRuntimeResponse expected = new LeonardoGetRuntimeResponse();
    expected.setRuntimeName("sample-cromwell-study");
    expected.setErrors(new ArrayList<>());
    expected.setRuntimeName("disk-123");
    expected.setStatus(LeonardoRuntimeStatus.RUNNING);
    expected.setCloudContext(new LeonardoCloudContext());

    LeonardoGetRuntimeResponse response = leoRuntimeService.getRuntime("googleProject", "runtimeName");

    assertEquals(expected, response);
  }

  static Map<String, String> contentTypeJsonHeader = Map.of("Content-Type", "application/json");
}
