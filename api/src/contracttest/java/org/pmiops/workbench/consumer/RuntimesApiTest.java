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
import org.pmiops.workbench.leonardo.model.LeonardoCreateRuntimeRequest;
import org.pmiops.workbench.leonardo.model.LeonardoGetRuntimeResponse;
import org.pmiops.workbench.leonardo.model.LeonardoRuntimeStatus;

@ExtendWith(PactConsumerTestExt.class)
class RuntimesApiTest {

  LeonardoCreateRuntimeRequest getLeonardoCreateRuntimeRequest() {
    LeonardoCreateRuntimeRequest request = new LeonardoCreateRuntimeRequest();
    request.setJupyterUserScriptUri("http://string.com");
    request.setJupyterStartUserScriptUri("http://start.com");
    request.setAutopause(true);
    request.setAutopauseThreshold(57);
    request.setDefaultClientId("string");
    request.setToolDockerImage("string");
    return request;
  }

  @Pact(consumer = "aou-rwb-api", provider = "leonardo")
  RequestResponsePact createDuplicateRuntime(PactDslWithProvider builder) {
    LeonardoCreateRuntimeRequest request = getLeonardoCreateRuntimeRequest();
    return builder
        .given("there is a runtime in a Google project")
        .uponReceiving("a request to create a runtime")
        .method("POST")
        .path("/api/google/v1/runtimes/googleProject/runtimename")
        .body(
            newJsonBody(
                    body -> {
                      body.stringType("jupyterUserScriptUri", "http://string.com");
                      body.stringType("jupyterStartUserScriptUri", "http://string.com");
                      body.booleanType("autopause", true);
                      body.numberType("autopauseThreshold", 57);
                      body.stringType("defaultClientId", "string");
                      body.stringType(
                          "toolDockerImage",
                          "us.gcr.io/broad-dsp-gcr-public/anvil-rstudio-bioconductor:3.18.0");
                    })
                .build())
        .willRespondWith()
        .status(409)
        .toPact();
  }

  @Pact(consumer = "aou-rwb-api", provider = "leonardo")
  RequestResponsePact createNewRuntime(PactDslWithProvider builder) {
    LeonardoCreateRuntimeRequest request = getLeonardoCreateRuntimeRequest();
    return builder
        .given("there is not a runtime in a Google project")
        .uponReceiving("a request to create a runtime")
        .method("POST")
        .path("/api/google/v1/runtimes/googleProject/runtimename")
        .body(
            newJsonBody(
                    body -> {
                      body.stringType("jupyterUserScriptUri", "http://string.com");
                      body.stringType("jupyterStartUserScriptUri", "http://string.com");
                      body.booleanType("autopause", true);
                      body.numberType("autopauseThreshold", 57);
                      body.stringType("defaultClientId", "string");
                      body.stringType(
                          "toolDockerImage",
                          "us.gcr.io/broad-dsp-gcr-public/anvil-rstudio-bioconductor:3.18.0");
                    })
                .build())
        .willRespondWith()
        .status(202)
        .toPact();
  }

  @Pact(consumer = "aou-rwb-api", provider = "leonardo")
  RequestResponsePact getRuntime(PactDslWithProvider builder) {
    return builder
        .given("there is a runtime in a Google project")
        .uponReceiving("a request to get that runtime")
        .method("GET")
        .path("/api/google/v1/runtimes/googleProject/runtimename")
        .willRespondWith()
        .status(200)
        .headers(contentTypeJsonHeader)
        .body(
            newJsonBody(
                    body -> {
                      body.stringType("runtimeName", "sample-cromwell-study");
                      body.stringType("status", "Running");
                      body.numberType("autopauseThreshold", 57);
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
                            context.stringType("dateAccessed", "Tuesday");
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
        .path("/api/google/v1/runtimes/googleProject/runtimename")
        .willRespondWith()
        .status(404)
        .headers(contentTypeJsonHeader)
        .body(newJsonBody(body -> {}).build())
        .toPact();
  }

  //  @Pact(consumer = "aou-rwb-api", provider = "leonardo")
  //  RequestResponsePact updateRuntime(PactDslWithProvider builder) {
  //    return builder
  //        .given("there is a runtime in a Google project")
  //        .uponReceiving("a request to get that runtime")
  //        .method("PATCH")
  //        .path("/api/google/v1/runtimes/googleProject/runtimename")
  //        .willRespondWith()
  //        .status(200)
  //        .headers(contentTypeJsonHeader)
  //        .body(
  //            newJsonBody(
  //                    body -> {
  //                      body.stringType("runtimeName", "sample-cromwell-study");
  //                      body.stringType("status", "Running");
  //                      body.stringType("autopauseThreshold", "57");
  //                      body.stringType("proxyUrl", "http://www.proxy.com");
  //                      body.array("errors", errors -> {});
  //                      body.object(
  //                          "cloudContext",
  //                          context -> {
  //                            context.stringType("cloudProvider", "GCP");
  //                            context.stringType("cloudResource", "terra-vpc-xx-fake-70e4eb32");
  //                          });
  //                      body.object(
  //                          "auditInfo",
  //                          context -> {
  //                            context.stringType("creator", "Bugs Bunny");
  //                            context.stringType("createdDate", "Yesterday");
  //                            context.stringType("dateAccessed", "Tuesday");
  //                          });
  //                    })
  //                .build())
  //        .toPact();
  //  }

  //  @Pact(consumer = "aou-rwb-api", provider = "leonardo")
  //  RequestResponsePact updateMissingRuntime(PactDslWithProvider builder) {
  //    return builder
  //        .given("there is not a runtime in a Google project")
  //        .uponReceiving("a request to get that runtime from GSuite")
  //        .method("PATCH")
  //        .path("/api/google/v1/runtimes/googleProject/runtimename")
  //        .willRespondWith()
  //        .status(404)
  //        .headers(contentTypeJsonHeader)
  //        .body(newJsonBody(body -> {}).build())
  //        .toPact();
  //  }

  @Test
  @PactTestFor(pactMethod = "createNewRuntime")
  void testCreateRuntimeWhenRuntimeDoesNotExist(MockServer mockServer) throws ApiException {
    ApiClient client = new ApiClient();
    client.setBasePath(mockServer.getUrl());
    RuntimesApi leoRuntimeService = new RuntimesApi(client);

    LeonardoCreateRuntimeRequest request = new LeonardoCreateRuntimeRequest();
    request.setJupyterUserScriptUri("http://string.com");
    request.setJupyterStartUserScriptUri("http://start.com");
    request.setAutopause(true);
    request.setAutopauseThreshold(57);
    request.setDefaultClientId("string");
    request.setToolDockerImage("us.gcr.io/broad-dsp-gcr-public/anvil-rstudio-bioconductor:3.18.0");

    leoRuntimeService.createRuntime("googleProject", "runtimename", request);
  }

  @Test
  @PactTestFor(pactMethod = "createDuplicateRuntime")
  void testCreateRuntimeWhenRuntimeDoesExist(MockServer mockServer) throws ApiException {
    ApiClient client = new ApiClient();
    client.setBasePath(mockServer.getUrl());
    RuntimesApi leoRuntimeService = new RuntimesApi(client);

    LeonardoCreateRuntimeRequest request = new LeonardoCreateRuntimeRequest();
    request.setJupyterUserScriptUri("http://string.com");
    request.setJupyterStartUserScriptUri("http://start.com");
    request.setAutopause(true);
    request.setAutopauseThreshold(57);
    request.setDefaultClientId("string");
    request.setToolDockerImage("us.gcr.io/broad-dsp-gcr-public/anvil-rstudio-bioconductor:3.18.0");

    assertThrows(
        Exception.class,
        () -> {
          leoRuntimeService.createRuntime("googleProject", "runtimename", request);
        });
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
        leoRuntimeService.getRuntime("googleProject", "runtimename");

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
            ApiException.class, () -> leoRuntimeService.getRuntime("googleProject", "runtimename"));

    assertEquals(exception.getMessage(), "Not Found");
  }

  //  @Test
  //  @PactTestFor(pactMethod = "updateMissingRuntime")
  //  void testUpdateRuntimeWhenRuntimeDoesNotExist(MockServer mockServer) throws ApiException {
  //    ApiClient client = new ApiClient();
  //    client.setBasePath(mockServer.getUrl());
  //    RuntimesApi leoRuntimeService = new RuntimesApi(client);
  //
  //    leoRuntimeService.updateRuntime("googleProject", "n", null);
  //  }

  //  @Test
  //  @PactTestFor(pactMethod = "updateRuntime")
  //  void testUpdateRuntimeWhenRuntimeDoesExist(MockServer mockServer) throws ApiException {
  //    ApiClient client = new ApiClient();
  //    client.setBasePath(mockServer.getUrl());
  //    RuntimesApi leoRuntimeService = new RuntimesApi(client);
  //
  //    leoRuntimeService.updateRuntime("googleProject", "runtimename", null);
  //  }

  static Map<String, String> contentTypeJsonHeader = Map.of("Content-Type", "application/json");
}
