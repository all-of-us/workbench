package org.pmiops.workbench.consumer;

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
import java.util.Map;
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
        .body(newJsonBody(body -> {
              body.numberType("id", 3);
              body.object("cloudContext", cloudContext -> {
                cloudContext.stringType("cloudProvider");
                cloudContext.stringType("cloudResource");
              });
              body.stringType("zone");
              body.stringType("name", DISK_NAME);
              body.stringType("samResourceId");
              body.stringMatcher("status", "Ready|Deleting|Error", "Ready");
              body.object("auditInfo", auditInfo -> {
                auditInfo.stringType("creator");
                auditInfo.stringType("createdDate", "2021-01-01T00:00:00Z");
                auditInfo.stringType("dateAccessed", "2021-01-01T00:00:00Z");
              });
              body.numberType("size");
              body.stringMatcher("diskType", "pd-standard|pd-ssd|pd-balanced", "pd-ssd");
              body.numberType("blockSize");
              body.object("labels", labels -> {
              });
        }).build())
        .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "getDisk")
  void testGetDiskWhenDiskExists(MockServer mockServer) throws ApiException {
    ApiClient client = new ApiClient();
    client.setBasePath(mockServer.getUrl());
    DisksApi api = new DisksApi(client);

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
  void testGetDiskWhenDiskDoesNotExist(MockServer mockServer) throws ApiException {
    ApiClient client = new ApiClient();
    client.setBasePath(mockServer.getUrl());
    DisksApi api = new DisksApi(client);

    ApiException exception =
        assertThrows(
            ApiException.class,
            () -> api.getDisk(GOOGLE_PROJECT, DISK_NAME));
    assertEquals("Not Found", exception.getMessage());
  }

  @Pact(consumer = "aou-rwb-api", provider = "leonardo")
  RequestResponsePact updateDisk(PactDslWithProvider builder) {
    return builder
        .given("there is a disk in a Google project")
        .uponReceiving("a request to update a disk")
        .method("PATCH")
        .path(DISK_ENDPOINT)
        .body(newJsonBody(body -> {
          body.numberType("size");
          body.stringMatcher("diskType", "pd-standard|pd-ssd|pd-balanced", "pd-ssd");
          body.numberType("blockSize");
          body.object("labels", labels -> {
          });
        }).build())
        .willRespondWith()
        .status(202)
        .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "updateDisk")
  void testUpdateDiskWhenDiskExists(MockServer mockServer) throws ApiException {
    ApiClient client = new ApiClient();
    client.setBasePath(mockServer.getUrl());
    DisksApi api = new DisksApi(client);

    LeonardoUpdateDiskRequest updateDiskRequest = new LeonardoUpdateDiskRequest();
    updateDiskRequest.setSize(100);
    updateDiskRequest.setDiskType(LeonardoDiskType.SSD);
    updateDiskRequest.setBlockSize(4096);
    updateDiskRequest.setLabels(Map.of());

    assertDoesNotThrow(() -> api.updateDisk(GOOGLE_PROJECT, DISK_NAME,updateDiskRequest));
  }

  @Pact(consumer = "aou-rwb-api", provider = "leonardo")
  RequestResponsePact updateMissingDisk(PactDslWithProvider builder) {
    return builder
        .given("there is a disk in a Google project")
        .uponReceiving("a request to update a disk")
        .method("PATCH")
        .path(DISK_ENDPOINT)
        .body(newJsonBody(body -> {
          body.numberType("size");
          body.stringMatcher("diskType", "pd-standard|pd-ssd|pd-balanced", "pd-ssd");
          body.numberType("blockSize");
          body.object("labels", labels -> {
          });
        }).build())
        .willRespondWith()
        .status(404)
        .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "updateMissingDisk")
  void testUpdateDiskWhenDiskDoesNotExist(MockServer mockServer) throws ApiException {
    ApiClient client = new ApiClient();
    client.setBasePath(mockServer.getUrl());
    DisksApi api = new DisksApi(client);

    LeonardoUpdateDiskRequest updateDiskRequest = new LeonardoUpdateDiskRequest();
    updateDiskRequest.setSize(100);
    updateDiskRequest.setDiskType(LeonardoDiskType.SSD);
    updateDiskRequest.setBlockSize(4096);
    updateDiskRequest.setLabels(Map.of());


    ApiException exception =
        assertThrows(
            ApiException.class,
            () -> api.updateDisk(GOOGLE_PROJECT, DISK_NAME,updateDiskRequest));
    assertEquals(exception.getMessage(), "Not Found");
  }

  static Map<String, String> contentTypeJsonHeader = Map.of("Content-Type", "application/json");
  static Map<String, String> contentTypeTextPlainHeader = Map.of("Content-Type", "text/plain");
}
