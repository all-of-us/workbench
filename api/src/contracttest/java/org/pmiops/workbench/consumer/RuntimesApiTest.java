package org.pmiops.workbench.consumer;

import static io.pactfoundation.consumer.dsl.LambdaDsl.newJsonBody;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import org.pmiops.workbench.leonardo.model.LeonardoAuditInfo;
import org.pmiops.workbench.leonardo.model.LeonardoCloudContext;
import org.pmiops.workbench.leonardo.model.LeonardoCloudProvider;
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
        .path("/api/google/v1/runtimes/googleProject/runtimeName")
        .willRespondWith()
        .status(200)
        .headers(contentTypeJsonHeader)
        .body(
            newJsonBody(
                    body -> {
                      body.stringType("runtimeName", "sample-cromwell-study");
                      body.stringType("status", "Running");
                      body.stringType("autopauseThreshold", "57");
                      body.stringType("proxyUrl", "http://www.proxy.com");
                      body.array("errors", errors -> {});
                      body.object(
                          "cloudContext",
                          context -> {
                            context.stringType("cloudProvider", "GCP");
                            context.stringType("cloudResource", "terra-vpc-xx-fake-70e4eb32");
                          });
                      body.object(
                          "auditInfo",
                          context -> {
                            context.stringType("creator", "Bugs Bunny");
                            context.stringType("createdDate", "Yesterday");
                            context.stringType("dateAccessed", "Wednesday");
                          });
                    })
                .build())
        .toPact();
  }

  @Pact(consumer = "aou-rwb-api", provider = "leonardo")
  RequestResponsePact getMissingRuntime(PactDslWithProvider builder) {
    return builder
        .given("there is not a runtime in a Google project")
        .uponReceiving("a request to get that runtime")
        .method("GET")
        .path("/api/google/v1/runtimes/googleProject/runtimeName")
        .willRespondWith()
        .status(404)
        .headers(contentTypeJsonHeader)
        .body(newJsonBody(body -> {}).build())
        .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "getRuntime")
  void testGetRuntimeWhenRuntimeExists(MockServer mockServer) throws ApiException {
    ApiClient client = new ApiClient();
    client.setBasePath(mockServer.getUrl());
    RuntimesApi leoRuntimeService = new RuntimesApi(client);

    LeonardoGetRuntimeResponse expected = new LeonardoGetRuntimeResponse();
    expected.setAutopauseThreshold(57);

    LeonardoAuditInfo auditInfo = new LeonardoAuditInfo();
    auditInfo.setCreator("Bugs Bunny");
    auditInfo.setCreatedDate("Yesterday");
    auditInfo.setDateAccessed("Tuesday");

    LeonardoCloudContext cloudContext = new LeonardoCloudContext();
    cloudContext.setCloudProvider(LeonardoCloudProvider.GCP);
    cloudContext.setCloudResource("terra-vpc-xx-fake-70e4eb32");

    expected.setAuditInfo(auditInfo);
    expected.setCloudContext(cloudContext);
    expected.setRuntimeName("sample-cromwell-study");
    expected.setErrors(new ArrayList<>());
    expected.setStatus(LeonardoRuntimeStatus.RUNNING);
    expected.setProxyUrl("http://www.proxy.com");

    LeonardoGetRuntimeResponse response =
        leoRuntimeService.getRuntime("googleProject", "runtimeName");

    assertEquals(expected, response);
  }

  @Test
  @PactTestFor(pactMethod = "getMissingRuntime")
  void testGetRuntimeWhenRuntimeDoesNotExist(MockServer mockServer) {
    ApiClient client = new ApiClient();
    client.setBasePath(mockServer.getUrl());
    RuntimesApi leoRuntimeService = new RuntimesApi(client);

    ApiException exception =
        assertThrows(
            ApiException.class, () -> leoRuntimeService.getRuntime("googleProject", "runtimeName"));

    assertEquals(exception.getMessage(), "Not Found");
  }

  static Map<String, String> contentTypeJsonHeader = Map.of("Content-Type", "application/json");
}
