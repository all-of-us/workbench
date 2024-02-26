package org.pmiops.workbench.consumer;

import static io.pactfoundation.consumer.dsl.LambdaDsl.newJsonArray;
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

@ExtendWith(PactConsumerTestExt.class)
class AppsApiTest {

  @Pact(consumer = "aou-rwb-api", provider = "leonardo")
  RequestResponsePact createNewApp(PactDslWithProvider builder) {
    return builder
        .given("there is not an app in a Google project")
        .uponReceiving("a request to create an app")
        .method("POST")
        .path("/api/google/v1/apps/googleProject/appname")
        .headers(contentTypeJsonHeader)
        .body(
            newJsonBody(
                    body -> {
                      body.stringType("appType", "RSTUDIO");
                      body.stringType("allowedChartName", LeonardoAllowedChartName.RSTUDIO.name());
                      body.object("labels", labels -> labels.stringType("key1", "value1"));
                      body.stringType("descriptorPath", "descriptor/path");
                      body.object(
                          "diskConfig",
                          diskConfig -> {
                            diskConfig.stringType("diskType", "SSD");
                            diskConfig.numberType("size", 100);
                          });
                      body.array("customEnvironmentVariables", customEnvironmentVariables -> {});
                      body.array("extraArgs", extraArgs -> {});
                      body.object("kubernetesRuntimeConfig", kubernetesRuntimeConfig -> {});
                      body.stringType("workspaceId", "Workspace123");
                    })
                .build())
        .willRespondWith()
        .status(200)
        .headers(contentTypeJsonHeader)
        .body(
            newJsonBody(
                    body -> {
                      body.stringType("appName", "sample-app");
                      body.stringType("status", "RUNNING");
                      body.stringType("diskName", "disk-123");
                      body.stringType("appType", "RSTUDIO");
                      body.array("errors", errors -> {});
                      body.object(
                          "cloudContext", context -> context.stringType("cloudprovider", null));
                    })
                .build())
        .toPact();
  }

  @Pact(consumer = "aou-rwb-api", provider = "leonardo")
  RequestResponsePact createDuplicateApp(PactDslWithProvider builder) {
    return builder
        .given("there is an app in a Google project")
        .uponReceiving("a request to create an app")
        .method("POST")
        .path("/api/google/v1/apps/googleProject/appname")
        .headers(contentTypeJsonHeader)
        .body(
            newJsonBody(
                    body -> {
                      body.stringType("appType", "RSTUDIO");
                      body.stringType("allowedChartName", LeonardoAllowedChartName.RSTUDIO.name());
                      body.object("labels", labels -> labels.stringType("key1", "value1"));
                      body.stringType("descriptorPath", "descriptor/path");
                      body.object(
                          "diskConfig",
                          diskConfig -> {
                            diskConfig.stringType("diskType", "SSD");
                            diskConfig.numberType("size", 100);
                          });
                      body.array("customEnvironmentVariables", customEnvironmentVariables -> {});
                      body.array("extraArgs", extraArgs -> {});
                      body.object("kubernetesRuntimeConfig", kubernetesRuntimeConfig -> {});
                      body.stringType("workspaceId", "Workspace123");
                    })
                .build())
        .willRespondWith()
        .status(409)
        .headers(contentTypeJsonHeader)
        .toPact();
  }

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

  @Pact(consumer = "aou-rwb-api", provider = "leonardo")
  RequestResponsePact getMissingApp(PactDslWithProvider builder) {
    return builder
        .given("there is not an app in a Google project")
        .uponReceiving("a request to get that app")
        .method("GET")
        .path("/api/google/v1/apps/googleProject/appname")
        .willRespondWith()
        .status(404)
        .headers(contentTypeJsonHeader)
        .body(newJsonBody(body -> {}).build())
        .toPact();
  }

  @Pact(consumer = "aou-rwb-api", provider = "leonardo")
  RequestResponsePact deleteApp(PactDslWithProvider builder) {
    return builder
        .given("there is an app in a Google project")
        .uponReceiving("a request to delete an app")
        .method("DELETE")
        .path("/api/google/v1/apps/googleProject/appname")
        .query("deleteDisk=false")
        .willRespondWith()
        .status(200)
        .headers(contentTypeJsonHeader)
        .body(newJsonBody(body -> {}).build())
        .toPact();
  }

  @Pact(consumer = "aou-rwb-api", provider = "leonardo")
  RequestResponsePact deleteMissingApp(PactDslWithProvider builder) {
    return builder
        .given("there is not an app in a Google project")
        .uponReceiving("a request to delete an app")
        .method("DELETE")
        .path("/api/google/v1/apps/googleProject/appname")
        .query("deleteDisk=false")
        .willRespondWith()
        .status(404)
        .headers(contentTypeJsonHeader)
        .body(newJsonBody(body -> {}).build())
        .toPact();
  }

  @Pact(consumer = "aou-rwb-api", provider = "leonardo")
  RequestResponsePact listAppsByProject(PactDslWithProvider builder) {
    return builder
        .given("there is a Google project")
        .uponReceiving("a request to list apps")
        .method("GET")
        .path("/api/google/v1/apps/googleProject")
        .matchQuery("includeDeleted", "false")
        .matchQuery("includeLabels", "")
        .matchQuery("role", "creator")
        .willRespondWith()
        .status(200)
        .headers(contentTypeJsonHeader)
        .body(newJsonArray(array -> {}).build())
        .toPact();
  }

  @Pact(consumer = "aou-rwb-api", provider = "leonardo")
  RequestResponsePact listAppsByMissingProject(PactDslWithProvider builder) {
    return builder
        .given("there is not a Google project")
        .uponReceiving("a request to list apps")
        .method("GET")
        .path("/api/google/v1/apps/googleProject")
        .matchQuery("includeDeleted", "false")
        .matchQuery("includeLabels", "")
        .matchQuery("role", "creator")
        .willRespondWith()
        .status(404)
        .headers(contentTypeJsonHeader)
        .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "createNewApp")
  void testCreateAppWhenAppDoesNotExist(MockServer mockServer) throws ApiException {
    ApiClient client = new ApiClient();
    client.setBasePath(mockServer.getUrl());
    AppsApi api = new AppsApi(client);

    LeonardoCreateAppRequest request = new LeonardoCreateAppRequest();
    request.setAppType(LeonardoAppType.RSTUDIO);
    request.setAllowedChartName(LeonardoAllowedChartName.RSTUDIO);
    request.setLabels(Map.ofEntries(entry("key1", "value1")));
    request.setDescriptorPath("descriptor/path");
    request.setDiskConfig(
        new LeonardoPersistentDiskRequest().diskType(LeonardoDiskType.SSD).size(100));
    request.setCustomEnvironmentVariables(new ArrayList<>());
    request.setExtraArgs(new ArrayList<>());
    request.setKubernetesRuntimeConfig(new LeonardoKubernetesRuntimeConfig());
    request.setWorkspaceId("Workspace123");

    api.createApp("googleProject", "appname", request);
  }

  @Test
  @PactTestFor(pactMethod = "createDuplicateApp")
  void testCreateAppWhenAppDoesExist(MockServer mockServer) throws ApiException {
    ApiClient client = new ApiClient();
    client.setBasePath(mockServer.getUrl());
    AppsApi api = new AppsApi(client);

    LeonardoCreateAppRequest request = new LeonardoCreateAppRequest();
    request.setAppType(LeonardoAppType.RSTUDIO);
    request.setAllowedChartName(LeonardoAllowedChartName.RSTUDIO);
    request.setLabels(Map.ofEntries(entry("key1", "value1")));
    request.setDescriptorPath("descriptor/path");
    request.setDiskConfig(
        new LeonardoPersistentDiskRequest().diskType(LeonardoDiskType.SSD).size(100));
    request.setCustomEnvironmentVariables(new ArrayList<>());
    request.setExtraArgs(new ArrayList<>());
    request.setKubernetesRuntimeConfig(new LeonardoKubernetesRuntimeConfig());
    request.setWorkspaceId("Workspace123");

    assertThrows(Exception.class, () -> api.createApp("googleProject", "appname", request));
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

    LeonardoGetAppResponse response = api.getApp("googleProject", "appname");

    assertEquals(expected, response);
  }

  @Test
  @PactTestFor(pactMethod = "getMissingApp")
  void testGetAppWhenAppDoesNotExist(MockServer mockServer) {
    ApiClient client = new ApiClient();
    client.setBasePath(mockServer.getUrl());
    AppsApi api = new AppsApi(client);

    ApiException exception =
        assertThrows(ApiException.class, () -> api.getApp("googleProject", "appname"));

    assertEquals(exception.getMessage(), "Not Found");
  }

  @Test
  @PactTestFor(pactMethod = "deleteApp")
  void testDeleteAppWhenAppExists(MockServer mockServer) throws ApiException {
    ApiClient client = new ApiClient();
    client.setBasePath(mockServer.getUrl());
    AppsApi api = new AppsApi(client);

    api.deleteApp("googleProject", "appname", false);
  }

  @Test
  @PactTestFor(pactMethod = "deleteMissingApp")
  void testDeleteAppWhenAppDoesNotExist(MockServer mockServer) {
    ApiClient client = new ApiClient();
    client.setBasePath(mockServer.getUrl());
    AppsApi api = new AppsApi(client);

    ApiException exception =
        assertThrows(ApiException.class, () -> api.deleteApp("googleProject", "appname", false));

    assertEquals(exception.getMessage(), "Not Found");
  }

  @Test
  @PactTestFor(pactMethod = "listAppsByProject")
  void testListAppWhenGoogleProjectExists(MockServer mockServer) throws ApiException {
    ApiClient client = new ApiClient();
    client.setBasePath(mockServer.getUrl());
    AppsApi api = new AppsApi(client);

    api.listAppByProject("googleProject", null, false, "", "creator");
  }

  @Test
  @PactTestFor(pactMethod = "listAppsByMissingProject")
  void testListAppWhenGoogleProjectDoesNotExist(MockServer mockServer) throws ApiException {
    ApiClient client = new ApiClient();
    client.setBasePath(mockServer.getUrl());
    AppsApi api = new AppsApi(client);

    ApiException exception =
        assertThrows(ApiException.class, () -> api.listAppByProject("googleProject", null, false, "", "creator"));

    assertEquals(exception.getMessage(), "Not Found");
  }

  static Map<String, String> contentTypeJsonHeader = Map.of("Content-Type", "application/json");
}
