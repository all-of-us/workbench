package org.pmiops.workbench.consumer;

import static io.pactfoundation.consumer.dsl.LambdaDsl.newJsonArray;
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
import io.pactfoundation.consumer.dsl.LambdaDslObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.pmiops.workbench.legacy_leonardo_client.ApiClient;
import org.pmiops.workbench.legacy_leonardo_client.ApiException;
import org.pmiops.workbench.legacy_leonardo_client.api.AppsApi;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoAllowedChartName;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoAppStatus;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoAppType;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoCloudContext;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoCreateAppRequest;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoDiskType;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoGetAppResponse;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoKubernetesRuntimeConfig;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoPersistentDiskRequest;
import org.pmiops.workbench.model.AppStatus;
import org.pmiops.workbench.model.AppType;

@ExtendWith(PactConsumerTestExt.class)
class AppsApiTest {
  private static final String GOOGLE_PROJECT = "googleproject";
  private static final String APP_NAME = "appname";
  private static final String APP_ENDPOINT =
      String.format("/api/google/v1/apps/%s/%s", GOOGLE_PROJECT, APP_NAME);
  private static final String LIST_APPS_ENDPOINT =
      String.format("/api/google/v1/apps/%s", GOOGLE_PROJECT);

  private AppsApi api;

  @BeforeEach
  void setUp(MockServer mockServer) {
    ApiClient client = new ApiClient();
    client.setBasePath(mockServer.getUrl());
    api = new AppsApi(client);
  }

  static LambdaDslJsonBody createAppRequestBody =
      newJsonBody(
          body -> {
            body.stringType("appType", AppType.RSTUDIO.name());
            body.stringType("allowedChartName", LeonardoAllowedChartName.RSTUDIO.toString());
            body.object("labels", labels -> labels.stringType("key1", "value1"));
            body.stringType("descriptorPath");
            body.object(
                "diskConfig",
                diskConfig -> {
                  diskConfig.stringType("diskType", LeonardoDiskType.SSD.toString());
                  diskConfig.numberType("size");
                  diskConfig.stringType("name", "mock-disk");
                });
            body.object(
                "customEnvironmentVariables",
                customEnvironmentVariables -> customEnvironmentVariables.stringType("key1"));
            body.array(
                "extraArgs",
                extraArgs -> {
                  extraArgs.stringType("arg1");
                  extraArgs.stringType("arg2");
                });
            body.object(
                "kubernetesRuntimeConfig",
                kubernetesRuntimeConfig -> {
                  kubernetesRuntimeConfig.numberType("numNodes");
                  kubernetesRuntimeConfig.stringType("machineType");
                  kubernetesRuntimeConfig.booleanType("autoscalingEnabled");
                });
            body.uuid("workspaceId");
          });

  static LeonardoCreateAppRequest createAppRequest() {
    LeonardoCreateAppRequest request = new LeonardoCreateAppRequest();
    LeonardoKubernetesRuntimeConfig runtimeConfig = new LeonardoKubernetesRuntimeConfig();
    runtimeConfig.setNumNodes(1);
    runtimeConfig.setMachineType("n1-standard-4");
    runtimeConfig.setAutoscalingEnabled(true);
    request.setAppType(LeonardoAppType.RSTUDIO);
    request.setAllowedChartName(LeonardoAllowedChartName.RSTUDIO);
    request.setLabels(Map.ofEntries(entry("key1", "value1")));
    request.setDescriptorPath("descriptor/path");
    request.setDiskConfig(
        new LeonardoPersistentDiskRequest()
            .diskType(LeonardoDiskType.SSD)
            .size(100)
            .name("mockDisk"));
    request.setCustomEnvironmentVariables(Map.ofEntries(entry("key1", "value1")));
    request.setExtraArgs(Arrays.asList("arg1", "arg2"));
    request.setKubernetesRuntimeConfig(runtimeConfig);
    request.setWorkspaceId(UUID.randomUUID().toString());
    return request;
  }

  void applyAppExpectations(LambdaDslObject app) {
    app.stringType("appName", "sample-cromwell-study");
    app.stringType("status", AppStatus.RUNNING.name());
    app.stringType("diskName", "disk-123");
    app.stringType("appType", "CROMWELL");
    app.array("errors", errors -> {});
    app.object("cloudContext", context -> context.stringType("cloudprovider", null));
  }

  @Pact(consumer = "aou-rwb-api", provider = "leonardo")
  RequestResponsePact createNewApp(PactDslWithProvider builder) {
    return builder
        .given("there is not an app in a Google project")
        .uponReceiving("a request to create an app")
        .method("POST")
        .path(APP_ENDPOINT)
        .headers(contentTypeJsonHeader)
        .body(createAppRequestBody.build())
        .willRespondWith()
        .status(202)
        .headers(contentTypeTextPlainHeader)
        .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "createNewApp")
  void testCreateAppWhenAppDoesNotExist() {
    LeonardoCreateAppRequest request = createAppRequest();

    assertDoesNotThrow(() -> api.createApp(GOOGLE_PROJECT, APP_NAME, request));
  }

  @Pact(consumer = "aou-rwb-api", provider = "leonardo")
  RequestResponsePact createDuplicateApp(PactDslWithProvider builder) {
    return builder
        .given("there is an app in a Google project")
        .uponReceiving("a request to create an app")
        .method("POST")
        .path(APP_ENDPOINT)
        .headers(contentTypeJsonHeader)
        .body(createAppRequestBody.build())
        .willRespondWith()
        .status(409)
        .headers(contentTypeJsonHeader)
        .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "createDuplicateApp")
  void testCreateAppWhenAppDoesExist() {
    LeonardoCreateAppRequest request = createAppRequest();

    assertThrows(Exception.class, () -> api.createApp(GOOGLE_PROJECT, APP_NAME, request));
  }

  @Pact(consumer = "aou-rwb-api", provider = "leonardo")
  RequestResponsePact getApp(PactDslWithProvider builder) {
    return builder
        .given("there is an app in a Google project")
        .uponReceiving("a request to get that app")
        .method("GET")
        .path(APP_ENDPOINT)
        .willRespondWith()
        .status(200)
        .headers(contentTypeJsonHeader)
        .body(newJsonBody(this::applyAppExpectations).build())
        .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "getApp")
  void testGetAppWhenAppExists() throws ApiException {
    LeonardoGetAppResponse expected = new LeonardoGetAppResponse();
    expected.setAppName("sample-cromwell-study");
    expected.setErrors(new ArrayList<>());
    expected.setDiskName("disk-123");
    expected.setStatus(LeonardoAppStatus.RUNNING);
    expected.setAppType(LeonardoAppType.CROMWELL);
    expected.setCloudContext(new LeonardoCloudContext());

    LeonardoGetAppResponse response = api.getApp(GOOGLE_PROJECT, APP_NAME);

    assertEquals(expected, response);
  }

  @Pact(consumer = "aou-rwb-api", provider = "leonardo")
  RequestResponsePact getMissingApp(PactDslWithProvider builder) {
    return builder
        .given("there is not an app in a Google project")
        .uponReceiving("a request to get that app")
        .method("GET")
        .path(APP_ENDPOINT)
        .willRespondWith()
        .status(404)
        .headers(contentTypeJsonHeader)
        .body(newJsonBody(body -> {}).build())
        .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "getMissingApp")
  void testGetAppWhenAppDoesNotExist() {
    ApiException exception =
        assertThrows(ApiException.class, () -> api.getApp(GOOGLE_PROJECT, APP_NAME));

    assertEquals(exception.getMessage(), "Not Found");
  }

  @Pact(consumer = "aou-rwb-api", provider = "leonardo")
  RequestResponsePact deleteApp(PactDslWithProvider builder) {
    return builder
        .given("there is an app in a Google project")
        .uponReceiving("a request to delete an app")
        .method("DELETE")
        .path(APP_ENDPOINT)
        .matchQuery("deleteDisk", "true|false")
        .willRespondWith()
        .status(202)
        .headers(contentTypeTextPlainHeader)
        .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "deleteApp")
  void testDeleteAppWhenAppExists() {
    assertDoesNotThrow(() -> api.deleteApp(GOOGLE_PROJECT, APP_NAME, false));
  }

  @Pact(consumer = "aou-rwb-api", provider = "leonardo")
  RequestResponsePact deleteMissingApp(PactDslWithProvider builder) {
    return builder
        .given("there is not an app in a Google project")
        .uponReceiving("a request to delete an app")
        .method("DELETE")
        .path(APP_ENDPOINT)
        .matchQuery("deleteDisk", "true|false")
        .willRespondWith()
        .status(404)
        .headers(contentTypeJsonHeader)
        .body(newJsonBody(body -> {}).build())
        .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "deleteMissingApp")
  void testDeleteAppWhenAppDoesNotExist() {
    ApiException exception =
        assertThrows(ApiException.class, () -> api.deleteApp(GOOGLE_PROJECT, APP_NAME, false));

    assertEquals(exception.getMessage(), "Not Found");
  }

  @Pact(consumer = "aou-rwb-api", provider = "leonardo")
  RequestResponsePact listAppsByProject(PactDslWithProvider builder) {
    return builder
        .given("there is a Google project with apps")
        .uponReceiving("a request to list apps")
        .method("GET")
        .path(LIST_APPS_ENDPOINT)
        .matchQuery("includeDeleted", "true|false")
        .matchQuery("includeLabels", ".*")
        .matchQuery("role", ".*")
        .willRespondWith()
        .status(200)
        .headers(contentTypeJsonHeader)
        .body(newJsonArray(apps -> apps.object(this::applyAppExpectations)).build())
        .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "listAppsByProject")
  void testListAppWhenGoogleProjectExists() {
    assertDoesNotThrow(() -> api.listAppByProject(GOOGLE_PROJECT, null, false, "", "creator"));
  }

  static Map<String, String> contentTypeJsonHeader = Map.of("Content-Type", "application/json");
  static Map<String, String> contentTypeTextPlainHeader = Map.of("Content-Type", "text/plain");
}
