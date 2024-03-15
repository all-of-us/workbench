package org.pmiops.workbench.consumer;

import static io.pactfoundation.consumer.dsl.LambdaDsl.newJsonBody;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

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
              body.stringType("googleId");
              body.stringType("samResourceId");
              body.stringMatcher("status", "READY|DELETING|ERROR", "READY");
              body.object("auditInfo", auditInfo -> {
                auditInfo.stringType("creator");
                auditInfo.stringType("createdDate", "2021-01-01T00:00:00Z");
                auditInfo.stringType("destroyedDate", "2021-01-04T00:00:00Z");
                auditInfo.stringType("dateAccessed", "2021-01-01T00:00:00Z");
                auditInfo.stringType("kernelFoundBusyDate", "2021-01-01T00:00:00Z");
              });
              body.numberType("size");
              body.stringMatcher("diskType", "SSD|HDD", "SSD");
              body.numberType("blockSize");
              body.object("labels", labels -> {
                labels.stringType("key1", "value1");
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

  static Map<String, String> contentTypeJsonHeader = Map.of("Content-Type", "application/json");
  static Map<String, String> contentTypeTextPlainHeader = Map.of("Content-Type", "text/plain");
}
