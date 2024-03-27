package org.pmiops.workbench.consumer;

import static io.pactfoundation.consumer.dsl.LambdaDsl.newJsonArray;
import static io.pactfoundation.consumer.dsl.LambdaDsl.newJsonBody;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import io.pactfoundation.consumer.dsl.LambdaDslObject;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.pmiops.workbench.leonardo.ApiClient;
import org.pmiops.workbench.leonardo.ApiException;
import org.pmiops.workbench.leonardo.api.DisksApi;
import org.pmiops.workbench.leonardo.model.LeonardoDiskType;
import org.pmiops.workbench.leonardo.model.LeonardoUpdateDiskRequest;

@ExtendWith(PactConsumerTestExt.class)
class DisksApiTest {
  private static final String GOOGLE_PROJECT = "googleproject";
  private static final String DISK_NAME = "diskname";

  private static final String DISK_ENDPOINT =
      String.format("/api/google/v1/disks/%s/%s", GOOGLE_PROJECT, DISK_NAME);
  private static final String LIST_DISKS_ENDPOINT =
      String.format("/api/google/v1/disks/%s", GOOGLE_PROJECT);

  private DisksApi api;

  @BeforeEach
  void setUp(MockServer mockServer) {
    ApiClient client = new ApiClient();
    client.setBasePath(mockServer.getUrl());
    api = new DisksApi(client);
  }

  void applyDiskExpectations(LambdaDslObject disk) {
    disk.numberType("id", 3);
    disk.object(
        "cloudContext",
        cloudContext -> {
          cloudContext.stringType("cloudProvider");
          cloudContext.stringType("cloudResource");
        });
    disk.stringType("zone");
    disk.stringType("name", DISK_NAME);
    disk.stringMatcher("status", "Ready|Deleting|Error", "Ready");
    disk.object(
        "auditInfo",
        auditInfo -> {
          auditInfo.stringType("creator");
          auditInfo.stringType("createdDate", "2021-01-01T00:00:00Z");
          auditInfo.stringType("dateAccessed", "2021-01-01T00:00:00Z");
        });
    disk.numberType("size");
    disk.stringMatcher("diskType", "pd-standard|pd-ssd|pd-balanced", "pd-ssd");
    disk.numberType("blockSize");
    disk.object("labels", labels -> {});
  }

  @Pact(consumer = "aou-rwb-api", provider = "leonardo")
  RequestResponsePact getDisk(PactDslWithProvider builder) {
    return builder
        .given("there is a disk in a Google project")
        .uponReceiving("a request to get a disk")
        .method("GET")
        .path(DISK_ENDPOINT)
        .headers(contentTypeJsonHeader)
        .willRespondWith()
        .status(200)
        .headers(contentTypeJsonHeader)
        .body(newJsonBody(this::applyDiskExpectations).build())
        .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "getDisk")
  void testGetDiskWhenDiskExists() {
    assertDoesNotThrow(() -> api.getDisk(GOOGLE_PROJECT, DISK_NAME));
  }

  @Pact(consumer = "aou-rwb-api", provider = "leonardo")
  RequestResponsePact getMissingDisk(PactDslWithProvider builder) {
    return builder
        .given("there is not a disk in a Google project")
        .uponReceiving("a request to get a disk")
        .method("GET")
        .path(DISK_ENDPOINT)
        .headers(contentTypeJsonHeader)
        .willRespondWith()
        .status(404)
        .headers(contentTypeJsonHeader)
        .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "getMissingDisk")
  void testGetDiskWhenDiskDoesNotExist() {
    ApiException exception =
        assertThrows(ApiException.class, () -> api.getDisk(GOOGLE_PROJECT, DISK_NAME));
    assertEquals("Not Found", exception.getMessage());
  }

  @Pact(consumer = "aou-rwb-api", provider = "leonardo")
  RequestResponsePact updateDisk(PactDslWithProvider builder) {
    return builder
        .given("there is a disk in a Google project")
        .uponReceiving("a request to update a disk")
        .method("PATCH")
        .path(DISK_ENDPOINT)
        .body(
            newJsonBody(
                    body -> {
                      body.numberType("size");
                      body.stringMatcher("diskType", "pd-standard|pd-ssd|pd-balanced", "pd-ssd");
                      body.numberType("blockSize");
                      body.object("labels", labels -> {});
                    })
                .build())
        .willRespondWith()
        .status(202)
        .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "updateDisk")
  void testUpdateDiskWhenDiskExists() {
    LeonardoUpdateDiskRequest updateDiskRequest = new LeonardoUpdateDiskRequest();
    updateDiskRequest.setSize(100);
    updateDiskRequest.setDiskType(LeonardoDiskType.SSD);
    updateDiskRequest.setBlockSize(4096);
    updateDiskRequest.setLabels(Map.of());

    assertDoesNotThrow(() -> api.updateDisk(GOOGLE_PROJECT, DISK_NAME, updateDiskRequest));
  }

  @Pact(consumer = "aou-rwb-api", provider = "leonardo")
  RequestResponsePact updateMissingDisk(PactDslWithProvider builder) {
    return builder
        .given("there is a disk in a Google project")
        .uponReceiving("a request to update a disk")
        .method("PATCH")
        .path(DISK_ENDPOINT)
        .body(
            newJsonBody(
                    body -> {
                      body.numberType("size");
                      body.stringMatcher("diskType", "pd-standard|pd-ssd|pd-balanced", "pd-ssd");
                      body.numberType("blockSize");
                      body.object("labels", labels -> {});
                    })
                .build())
        .willRespondWith()
        .status(404)
        .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "updateMissingDisk")
  void testUpdateDiskWhenDiskDoesNotExist() {
    LeonardoUpdateDiskRequest updateDiskRequest = new LeonardoUpdateDiskRequest();
    updateDiskRequest.setSize(100);
    updateDiskRequest.setDiskType(LeonardoDiskType.SSD);
    updateDiskRequest.setBlockSize(4096);
    updateDiskRequest.setLabels(Map.of());

    ApiException exception =
        assertThrows(
            ApiException.class, () -> api.updateDisk(GOOGLE_PROJECT, DISK_NAME, updateDiskRequest));
    assertEquals(exception.getMessage(), "Not Found");
  }

  @Pact(consumer = "aou-rwb-api", provider = "leonardo")
  RequestResponsePact listDisksByProject(PactDslWithProvider builder) {
    return builder
        .given("there is a Google project with disks")
        .uponReceiving("a request to list disks by project")
        .method("GET")
        .path(LIST_DISKS_ENDPOINT)
        .matchQuery("includeDeleted", "true|false")
        .matchQuery("includeLabels", ".*")
        .matchQuery("role", ".*")
        .willRespondWith()
        .status(200)
        .headers(contentTypeJsonHeader)
        .body(newJsonArray(apps -> apps.object(this::applyDiskExpectations)).build())
        .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "listDisksByProject")
  void testListDisksByProjectWhenGoogleProjectExists() {
    assertDoesNotThrow(() -> api.listDisksByProject(GOOGLE_PROJECT, null, true, "AOU", "creator"));
  }

  static Map<String, String> contentTypeJsonHeader = Map.of("Content-Type", "application/json");
}
