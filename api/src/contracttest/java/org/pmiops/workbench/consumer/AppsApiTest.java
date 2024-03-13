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
import java.util.ArrayList;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.pmiops.workbench.leonardo.ApiClient;
import org.pmiops.workbench.leonardo.ApiException;
import org.pmiops.workbench.leonardo.api.AppsApi;
import org.pmiops.workbench.leonardo.model.LeonardoAllowedChartName;
import org.pmiops.workbench.leonardo.model.LeonardoAppStatus;
import org.pmiops.workbench.leonardo.model.LeonardoAppType;
import org.pmiops.workbench.leonardo.model.LeonardoCloudContext;
import org.pmiops.workbench.leonardo.model.LeonardoCreateAppRequest;
import org.pmiops.workbench.leonardo.model.LeonardoDiskType;
import org.pmiops.workbench.leonardo.model.LeonardoGetAppResponse;
import org.pmiops.workbench.leonardo.model.LeonardoKubernetesRuntimeConfig;
import org.pmiops.workbench.leonardo.model.LeonardoPersistentDiskRequest;
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

  static LambdaDslJsonBody createAppRequestBody =
      newJsonBody(
          body -> {
            body.stringType("appType", AppType.RSTUDIO.name());
            body.stringType("allowedChartName", LeonardoAllowedChartName.RSTUDIO.name());
            body.object("labels", labels -> labels.stringType("key1", "value1"));
            body.stringType("descriptorPath");
            body.object(
                "diskConfig",
                diskConfig -> {
                  diskConfig.stringType("diskType", LeonardoDiskType.SSD.name());
                  diskConfig.numberType("size");
                });
            body.array("customEnvironmentVariables", customEnvironmentVariables -> {});
            body.array("extraArgs", extraArgs -> {});
            body.object(
                "kubernetesRuntimeConfig",
                kubernetesRuntimeConfig -> {
                  kubernetesRuntimeConfig.numberType("numNodes");
                  kubernetesRuntimeConfig.stringType("machineType");
                  kubernetesRuntimeConfig.booleanType("autoscalingEnabled");
                });
            body.stringType("workspaceId");
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
        new LeonardoPersistentDiskRequest().diskType(LeonardoDiskType.SSD).size(100));
    request.setCustomEnvironmentVariables(new ArrayList<>());
    request.setExtraArgs(new ArrayList<>());
    request.setKubernetesRuntimeConfig(runtimeConfig);
    request.setWorkspaceId("Workspace123");
    return request;
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
        .headers(contentTypeJsonHeader)
        .body(
            newJsonBody(
                    body -> {
                      body.stringType("appName");
                      body.stringType("status", AppStatus.RUNNING.name());
                      body.stringType("diskName");
                      body.stringType("appType", AppType.RSTUDIO.name());
                      body.array("errors", errors -> {});
                      body.object(
                          "cloudContext", context -> context.stringType("cloudprovider", null));
                    })
                .build())
        .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "createNewApp")
  void testCreateAppWhenAppDoesNotExist(MockServer mockServer) throws ApiException {
    ApiClient client = new ApiClient();
    client.setBasePath(mockServer.getUrl());
    AppsApi api = new AppsApi(client);

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
  void testCreateAppWhenAppDoesExist(MockServer mockServer) throws ApiException {
    ApiClient client = new ApiClient();
    client.setBasePath(mockServer.getUrl());
    AppsApi api = new AppsApi(client);

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
        .body(
            newJsonBody(
                    body -> {
                      body.stringType("appName", "sample-cromwell-study");
                      body.stringType("status", AppStatus.RUNNING.name());
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
    AppsApi api = new AppsApi(client);

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
  void testGetAppWhenAppDoesNotExist(MockServer mockServer) {
    ApiClient client = new ApiClient();
    client.setBasePath(mockServer.getUrl());
    AppsApi api = new AppsApi(client);

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
        .status(200)
        .headers(contentTypeJsonHeader)
        .body(newJsonBody(body -> {}).build())
        .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "deleteApp")
  void testDeleteAppWhenAppExists(MockServer mockServer) throws ApiException {
    ApiClient client = new ApiClient();
    client.setBasePath(mockServer.getUrl());
    AppsApi api = new AppsApi(client);

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
  void testDeleteAppWhenAppDoesNotExist(MockServer mockServer) {
    ApiClient client = new ApiClient();
    client.setBasePath(mockServer.getUrl());
    AppsApi api = new AppsApi(client);

    ApiException exception =
        assertThrows(ApiException.class, () -> api.deleteApp(GOOGLE_PROJECT, APP_NAME, false));

    assertEquals(exception.getMessage(), "Not Found");
  }

  @Pact(consumer = "aou-rwb-api", provider = "leonardo")
  RequestResponsePact listAppsByProject(PactDslWithProvider builder) {
    return builder
        .given("there is a Google project")
        .uponReceiving("a request to list apps")
        .method("GET")
        .path(LIST_APPS_ENDPOINT)
        .matchQuery("includeDeleted", "true|false")
        .matchQuery("includeLabels", ".*")
        .matchQuery("role", ".*")
        .willRespondWith()
        .status(200)
        .headers(contentTypeJsonHeader)
        .body(newJsonArray(array -> {}).build())
        .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "listAppsByProject")
  void testListAppWhenGoogleProjectExists(MockServer mockServer) throws ApiException {
    ApiClient client = new ApiClient();
    client.setBasePath(mockServer.getUrl());
    AppsApi api = new AppsApi(client);

    assertDoesNotThrow(() -> api.listAppByProject(GOOGLE_PROJECT, null, false, "", "creator"));
  }

  @Pact(consumer = "aou-rwb-api", provider = "leonardo")
  RequestResponsePact listAppsByMissingProject(PactDslWithProvider builder) {
    return builder
        .given("there is not a Google project")
        .uponReceiving("a request to list apps")
        .method("GET")
        .path(LIST_APPS_ENDPOINT)
        .matchQuery("includeDeleted", "true|false")
        .matchQuery("includeLabels", ".*")
        .matchQuery("role", ".*")
        .willRespondWith()
        .status(404)
        .headers(contentTypeJsonHeader)
        .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "listAppsByMissingProject")
  void testListAppWhenGoogleProjectDoesNotExist(MockServer mockServer) throws ApiException {
    ApiClient client = new ApiClient();
    client.setBasePath(mockServer.getUrl());
    AppsApi api = new AppsApi(client);

    ApiException exception =
        assertThrows(
            ApiException.class,
            () -> api.listAppByProject(GOOGLE_PROJECT, null, false, "", "creator"));

    assertEquals(exception.getMessage(), "Not Found");
  }

  static Map<String, String> contentTypeJsonHeader = Map.of("Content-Type", "application/json");
}
