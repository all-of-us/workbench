package org.pmiops.workbench.consumer;

import static io.pactfoundation.consumer.dsl.LambdaDsl.newJsonBody;
import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import io.pactfoundation.consumer.dsl.LambdaDslJsonBody;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.pmiops.workbench.legacy_leonardo_client.ApiClient;
import org.pmiops.workbench.legacy_leonardo_client.ApiException;
import org.pmiops.workbench.legacy_leonardo_client.api.RuntimesApi;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoCreateRuntimeRequest;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoUpdateGceConfig;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoUpdateGceConfig.CloudServiceEnum;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoUpdateRuntimeRequest;

@ExtendWith(PactConsumerTestExt.class)
class RuntimesApiTest {

  static Map<String, String> contentTypeJsonHeader = Map.of("Content-Type", "application/json");
  static LambdaDslJsonBody createBody =
      newJsonBody(
          (body) -> {
            // Requires a valid URL, so an example is passed for use in tests
            body.stringType("jupyterUserScriptUri", "http://example.com");
            body.stringType("jupyterStartUserScriptUri", "http://example.com");
            body.booleanType("autopause");
            // Requires a value that within a specified range, so an example is passed for use in
            // tests.
            body.numberType("autopauseThreshold", 30);
            body.stringType("defaultClientId");
            // Requires a valid Docker image path, so an example is passed for use in tests
            body.stringType("toolDockerImage", "us.gcr.io/example/image-for-contract-test:2.2.7");
          });

  private RuntimesApi api;

  @BeforeEach
  void setUp(MockServer mockServer) {
    ApiClient client = new ApiClient();
    client.setBasePath(mockServer.getUrl());
    api = new RuntimesApi(client);
  }

  @Pact(consumer = "aou-rwb-api", provider = "leonardo")
  RequestResponsePact createDuplicateRuntime(PactDslWithProvider builder) {
    return builder
        .given("there is a runtime in a Google project")
        .uponReceiving("a request to create a runtime")
        .method("POST")
        .path("/api/google/v1/runtimes/googleProject/exampleruntimename")
        .body(createBody.build())
        .willRespondWith()
        .status(409)
        .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "createDuplicateRuntime")
  void testCreateRuntimeWhenRuntimeDoesExist() {
    LeonardoCreateRuntimeRequest request = new LeonardoCreateRuntimeRequest();
    request.setJupyterUserScriptUri("http://string.com");
    request.setJupyterStartUserScriptUri("http://start.com");
    request.setAutopause(true);
    request.setAutopauseThreshold(57);
    request.setDefaultClientId("string");

    request.setToolDockerImage("us.gcr.io/broad-dsp-gcr-public/anvil-rstudio-bioconductor:3.18.0");

    assertThrows(
        Exception.class, () -> api.createRuntime("googleProject", "exampleruntimename", request));
  }

  @Pact(consumer = "aou-rwb-api", provider = "leonardo")
  RequestResponsePact createNewRuntime(PactDslWithProvider builder) {
    return builder
        .given("there is not a runtime in a Google project")
        .uponReceiving("a request to create a runtime")
        .method("POST")
        .path("/api/google/v1/runtimes/googleProject/exampleruntimename")
        .body(createBody.build())
        .willRespondWith()
        .status(202)
        .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "createNewRuntime")
  void testCreateRuntimeWhenRuntimeDoesNotExist() {
    LeonardoCreateRuntimeRequest request = new LeonardoCreateRuntimeRequest();
    request.setJupyterUserScriptUri("http://string.com");
    request.setJupyterStartUserScriptUri("http://start.com");
    request.setAutopause(true);
    request.setAutopauseThreshold(57);
    request.setDefaultClientId("string");

    request.setToolDockerImage("us.gcr.io/broad-dsp-gcr-public/anvil-rstudio-bioconductor:3.18.0");

    assertDoesNotThrow(() -> api.createRuntime("googleProject", "exampleruntimename", request));
  }

  @Pact(consumer = "aou-rwb-api", provider = "leonardo")
  RequestResponsePact getRuntime(PactDslWithProvider builder) {
    return builder
        .given("there is a runtime in a Google project")
        .uponReceiving("a request to get a runtime")
        .method("GET")
        .path("/api/google/v1/runtimes/googleProject/exampleruntimename")
        .willRespondWith()
        .status(200)
        .headers(contentTypeJsonHeader)
        .body(
            newJsonBody(
                    body -> {
                      body.stringType("runtimeName");
                      body.stringType("status");
                      body.numberType("autopauseThreshold", 50);
                      body.stringType("proxyUrl");
                      body.array("errors", errors -> {});
                      body.object(
                          "cloudContext",
                          context -> {
                            context.stringType("cloudProvider");
                            context.stringType("cloudResource");
                          });
                      body.object(
                          "auditInfo",
                          context -> {
                            context.stringType("creator");
                            context.stringType("createdDate");
                            context.stringType("dateAccessed");
                          });
                    })
                .build())
        .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "getRuntime")
  void testGetRuntimeWhenRuntimeExists() {
    assertDoesNotThrow(() -> api.getRuntime("googleProject", "exampleruntimename"));
  }

  @Pact(consumer = "aou-rwb-api", provider = "leonardo")
  RequestResponsePact getMissingRuntime(PactDslWithProvider builder) {
    return builder
        .given("there is not a runtime in a Google project")
        .uponReceiving("a request to get a runtime")
        .method("GET")
        .path("/api/google/v1/runtimes/googleProject/exampleruntimename")
        .willRespondWith()
        .status(404)
        .headers(contentTypeJsonHeader)
        .body(newJsonBody(body -> {}).build())
        .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "getMissingRuntime")
  void testGetRuntimeWhenRuntimeDoesNotExist() {
    ApiException exception =
        assertThrows(
            ApiException.class, () -> api.getRuntime("googleProject", "exampleruntimename"));

    assertEquals(exception.getMessage(), "Not Found");
  }

  @Pact(consumer = "aou-rwb-api", provider = "leonardo")
  RequestResponsePact updateRuntime(PactDslWithProvider builder) {
    return builder
        .given("there is a runtime in a Google project")
        .uponReceiving("a request to update a runtime")
        .method("PATCH")
        .path("/api/google/v1/runtimes/googleProject/exampleruntimename")
        .body(
            newJsonBody(
                    body -> {
                      body.booleanType("allowStop");
                      body.booleanType("autopause");
                      body.numberType("autopauseThreshold", 30);
                    })
                .build())
        .willRespondWith()
        .status(202)
        .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "updateRuntime")
  void testUpdateRuntimeWhenRuntimeDoesExist() {
    LeonardoUpdateRuntimeRequest request = new LeonardoUpdateRuntimeRequest();
    request.setAllowStop(true);
    request.setAutopause(true);
    request.setAutopauseThreshold(200);

    assertDoesNotThrow(() -> api.updateRuntime("googleProject", "exampleruntimename", request));
  }

  @Pact(consumer = "aou-rwb-api", provider = "leonardo")
  RequestResponsePact updateMissingRuntime(PactDslWithProvider builder) {
    return builder
        .given("there is not a runtime in a Google project")
        .uponReceiving("a request to update a runtime")
        .method("PATCH")
        .path("/api/google/v1/runtimes/googleProject/exampleruntimename")
        .body(
            newJsonBody(
                    body -> {
                      body.booleanType("allowStop");
                      body.booleanType("autopause");
                      body.numberType("autopauseThreshold", 50);
                      body.object(
                          "runtimeConfig",
                          runtimeConfig -> {
                            runtimeConfig.stringMatcher(
                                "cloudService", "(DATAPROC|GCE)", CloudServiceEnum.GCE.name());
                            runtimeConfig.stringType("machineType");
                            runtimeConfig.numberType("diskSize");
                          });
                      body.array("labelsToDelete", arr -> arr.stringType("deletableLabel"));
                      body.object(
                          "labelsToUpsert",
                          labels -> labels.stringValue("randomKey", "randomValue"));
                    })
                .build())
        .willRespondWith()
        .status(404)
        .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "updateMissingRuntime")
  void testUpdateRuntimeWhenRuntimeDoesNotExist() {
    LeonardoUpdateRuntimeRequest request = new LeonardoUpdateRuntimeRequest();
    request.setAllowStop(true);
    request.setAutopause(true);
    request.setAutopauseThreshold(523);
    LeonardoUpdateGceConfig config = new LeonardoUpdateGceConfig();
    config.setCloudService(CloudServiceEnum.GCE);
    config.setDiskSize(500);
    config.setMachineType("n1-highmem-16");
    request.setRuntimeConfig(config);
    request.setLabelsToDelete(new ArrayList<>(List.of("deletableLabel")));
    request.setLabelsToUpsert(Map.ofEntries(entry("randomKey", "randomValue")));

    ApiException exception =
        assertThrows(
            ApiException.class,
            () -> api.updateRuntime("googleProject", "exampleruntimename", request));
    assertEquals("Not Found", exception.getMessage());
  }

  @Pact(consumer = "aou-rwb-api", provider = "leonardo")
  RequestResponsePact deleteRuntime(PactDslWithProvider builder) {
    return builder
        .given("there is a runtime in a Google project")
        .uponReceiving("a request to delete a runtime")
        .method("DELETE")
        .path("/api/google/v1/runtimes/googleProject/exampleruntimename")
        .query("deleteDisk=true")
        .willRespondWith()
        .status(202)
        .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "deleteRuntime")
  void testDeleteRuntimeWhenRuntimeDoesExist() {
    assertDoesNotThrow(() -> api.deleteRuntime("googleProject", "exampleruntimename", true));
  }

  @Pact(consumer = "aou-rwb-api", provider = "leonardo")
  RequestResponsePact deleteMissingRuntime(PactDslWithProvider builder) {
    return builder
        .given("there is not a runtime in a Google project")
        .uponReceiving("a request to delete a runtime")
        .method("DELETE")
        .path("/api/google/v1/runtimes/googleProject/exampleruntimename")
        .query("deleteDisk=true")
        .willRespondWith()
        .status(404)
        .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "deleteMissingRuntime")
  void testDeleteRuntimeWhenRuntimeDoesNotExist() {
    ApiException exception =
        assertThrows(
            ApiException.class,
            () -> api.deleteRuntime("googleProject", "exampleruntimename", true));
    assertEquals(exception.getMessage(), "Not Found");
  }

  @Pact(consumer = "aou-rwb-api", provider = "leonardo")
  RequestResponsePact stopRuntime(PactDslWithProvider builder) {
    return builder
        .given("there is a runtime in a Google project")
        .uponReceiving("a request to stop a runtime")
        .method("POST")
        .path("/api/google/v1/runtimes/googleProject/exampleruntimename/stop")
        .willRespondWith()
        .status(202)
        .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "stopRuntime")
  void testStopRuntimeWhenRuntimeDoesExist() {
    assertDoesNotThrow(() -> api.stopRuntime("googleProject", "exampleruntimename"));
  }

  //
  @Pact(consumer = "aou-rwb-api", provider = "leonardo")
  RequestResponsePact stopMissingRuntime(PactDslWithProvider builder) {
    return builder
        .given("there is not a runtime in a Google project")
        .uponReceiving("a request to stop a runtime")
        .method("POST")
        .path("/api/google/v1/runtimes/googleProject/exampleruntimename/stop")
        .willRespondWith()
        .status(404)
        .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "stopMissingRuntime")
  void testStopRuntimeWhenRuntimeDoesNotExist() {
    ApiException exception =
        assertThrows(
            ApiException.class, () -> api.stopRuntime("googleProject", "exampleruntimename"));
    assertEquals(exception.getMessage(), "Not Found");
  }
}
