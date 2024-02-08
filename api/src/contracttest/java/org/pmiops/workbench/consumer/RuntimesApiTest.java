package org.pmiops.workbench.consumer;

import static io.pactfoundation.consumer.dsl.LambdaDsl.newJsonBody;
import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.pmiops.workbench.leonardo.model.LeonardoUpdateGceConfig;
import org.pmiops.workbench.leonardo.model.LeonardoUpdateGceConfig.CloudServiceEnum;
import org.pmiops.workbench.leonardo.model.LeonardoUpdateRuntimeRequest;

@ExtendWith(PactConsumerTestExt.class)
class RuntimesApiTest {
  @Pact(consumer = "aou-rwb-api", provider = "leonardo")
  RequestResponsePact createDuplicateRuntime(PactDslWithProvider builder) {
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
        .uponReceiving("a request to get a runtime")
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
        .uponReceiving("a request to get a runtime")
        .method("GET")
        .path("/api/google/v1/runtimes/googleProject/runtimename")
        .willRespondWith()
        .status(404)
        .headers(contentTypeJsonHeader)
        .body(newJsonBody(body -> {}).build())
        .toPact();
  }

  @Pact(consumer = "aou-rwb-api", provider = "leonardo")
  RequestResponsePact updateRuntime(PactDslWithProvider builder) {
    return builder
        .given("there is a runtime in a Google project")
        .uponReceiving("a request to update a runtime")
        .method("PATCH")
        .path("/api/google/v1/runtimes/googleProject/runtimename")
        .body(
            newJsonBody(
                    body -> {
                      body.booleanType("allowStop", true);
                      body.booleanType("autopause", true);
                      body.numberType("autopauseThreshold", 57);
                    })
                .build())
        .willRespondWith()
        .status(202)
        .toPact();
  }

  @Pact(consumer = "aou-rwb-api", provider = "leonardo")
  RequestResponsePact updateMissingRuntime(PactDslWithProvider builder) {
    return builder
        .given("there is not a runtime in a Google project")
        .uponReceiving("a request to update a runtime")
        .method("PATCH")
        .path("/api/google/v1/runtimes/googleProject/runtimename")
        .body(
            newJsonBody(
                    body -> {
                      body.booleanType("allowStop", true);
                      body.booleanType("autopause", true);
                      body.numberType("autopauseThreshold", 523);
                      body.object(
                          "runtimeConfig",
                          runtimeConfig -> {
                            runtimeConfig.stringType("cloudService", "GCE");
                            runtimeConfig.stringType("machineType", "n1-highmem-16");
                            runtimeConfig.numberType("diskSize", 500);
                          });
                      body.array("labelsToDelete", arr -> arr.stringValue("deletableLabel"));
                      body.object(
                          "labelsToUpsert", labels -> labels.stringValue("key1", "ke1Updated"));
                    })
                .build())
        .willRespondWith()
        .status(404)
        .toPact();
  }

  @Pact(consumer = "aou-rwb-api", provider = "leonardo")
  RequestResponsePact deleteRuntime(PactDslWithProvider builder) {
    return builder
        .given("there is a runtime in a Google project")
        .uponReceiving("a request to delete a runtime")
        .method("DELETE")
        .path("/api/google/v1/runtimes/googleProject/runtimename")
        .query("deleteDisk=true")
        .willRespondWith()
        .status(202)
        .toPact();
  }

  @Pact(consumer = "aou-rwb-api", provider = "leonardo")
  RequestResponsePact deleteMissingRuntime(PactDslWithProvider builder) {
    return builder
        .given("there is not a runtime in a Google project")
        .uponReceiving("a request to delete a runtime")
        .method("DELETE")
        .path("/api/google/v1/runtimes/googleProject/runtimename")
        .query("deleteDisk=true")
        .willRespondWith()
        .status(404)
        .toPact();
  }

  @Pact(consumer = "aou-rwb-api", provider = "leonardo")
  RequestResponsePact stopRuntime(PactDslWithProvider builder) {
    return builder
        .given("there is a runtime in a Google project")
        .uponReceiving("a request to stop a runtime")
        .method("POST")
        .path("/api/google/v1/runtimes/googleProject/runtimename/stop")
        .willRespondWith()
        .status(202)
        .toPact();
  }

  @Pact(consumer = "aou-rwb-api", provider = "leonardo")
  RequestResponsePact stopMissingRuntime(PactDslWithProvider builder) {
    return builder
        .given("there is not a runtime in a Google project")
        .uponReceiving("a request to stop a runtime")
        .method("POST")
        .path("/api/google/v1/runtimes/googleProject/runtimename/stop")
        .willRespondWith()
        .status(404)
        .toPact();
  }

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
        () -> leoRuntimeService.createRuntime("googleProject", "runtimename", request));
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

  @Test
  @PactTestFor(pactMethod = "updateMissingRuntime")
  void testUpdateRuntimeWhenRuntimeDoesNotExist(MockServer mockServer) throws ApiException {
    ApiClient client = new ApiClient();
    client.setBasePath(mockServer.getUrl());
    RuntimesApi leoRuntimeService = new RuntimesApi(client);
    LeonardoUpdateRuntimeRequest request = new LeonardoUpdateRuntimeRequest();
    request.setAllowStop(true);
    request.setAutopause(true);
    request.setAutopauseThreshold(523);
    LeonardoUpdateGceConfig config = new LeonardoUpdateGceConfig();
    config.setCloudService(CloudServiceEnum.GCE);
    config.setDiskSize(500);
    config.setMachineType("n1-highmem-16");
    request.setRuntimeConfig(config);
    request.setLabelsToDelete(new ArrayList<>(Arrays.asList("deletableLabel")));
    request.setLabelsToUpsert(Map.ofEntries(entry("key1", "ke1Updated")));

    assertThrows(
        Exception.class,
        () -> leoRuntimeService.updateRuntime("googleProject", "runtimename", request));
  }

  @Test
  @PactTestFor(pactMethod = "updateRuntime")
  void testUpdateRuntimeWhenRuntimeDoesExist(MockServer mockServer) throws ApiException {
    ApiClient client = new ApiClient();
    client.setBasePath(mockServer.getUrl());
    RuntimesApi leoRuntimeService = new RuntimesApi(client);
    LeonardoUpdateRuntimeRequest request = new LeonardoUpdateRuntimeRequest();
    request.setAllowStop(true);
    request.setAutopause(true);
    request.setAutopauseThreshold(200);

    leoRuntimeService.updateRuntime("googleProject", "runtimename", request);
  }

  @Test
  @PactTestFor(pactMethod = "deleteRuntime")
  void testDeleteRuntimeWhenRuntimeDoesExist(MockServer mockServer) throws ApiException {
    ApiClient client = new ApiClient();
    client.setBasePath(mockServer.getUrl());
    RuntimesApi leoRuntimeService = new RuntimesApi(client);

    leoRuntimeService.deleteRuntime("googleProject", "runtimename", true);
  }

  @Test
  @PactTestFor(pactMethod = "deleteMissingRuntime")
  void testDeleteRuntimeWhenRuntimeDoesNotExist(MockServer mockServer) throws ApiException {
    ApiClient client = new ApiClient();
    client.setBasePath(mockServer.getUrl());
    RuntimesApi leoRuntimeService = new RuntimesApi(client);

    assertThrows(
        Exception.class,
        () -> leoRuntimeService.deleteRuntime("googleProject", "runtimename", true));
  }

  @Test
  @PactTestFor(pactMethod = "stopRuntime")
  void testStopRuntimeWhenRuntimeDoesExist(MockServer mockServer) throws ApiException {
    ApiClient client = new ApiClient();
    client.setBasePath(mockServer.getUrl());
    RuntimesApi leoRuntimeService = new RuntimesApi(client);

    leoRuntimeService.stopRuntime("googleProject", "runtimename");
  }

  @Test
  @PactTestFor(pactMethod = "stopMissingRuntime")
  void testStopRuntimeWhenRuntimeDoesNotExist(MockServer mockServer) throws ApiException {
    ApiClient client = new ApiClient();
    client.setBasePath(mockServer.getUrl());
    RuntimesApi leoRuntimeService = new RuntimesApi(client);

    assertThrows(
        Exception.class, () -> leoRuntimeService.stopRuntime("googleProject", "runtimename"));
  }

  static Map<String, String> contentTypeJsonHeader = Map.of("Content-Type", "application/json");
}
