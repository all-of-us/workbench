package org.pmiops.workbench.consumer;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.pmiops.workbench.notebooks.ApiClient;
import org.pmiops.workbench.notebooks.ApiException;
import org.pmiops.workbench.notebooks.api.ProxyApi;
import org.pmiops.workbench.notebooks.model.LocalizationEntry;
import org.pmiops.workbench.notebooks.model.Localize;

@ExtendWith(PactConsumerTestExt.class)
class ProxyApiTest {
  @Pact(consumer = "aou-rwb-api", provider = "leonardo")
  RequestResponsePact welderLocalize(PactDslWithProvider builder) {
    return builder
        .given("there is an object to localize")
        .uponReceiving("a request to localize an object")
        .method("POST")
        .path("/proxy/googleProject/runtimename/welder/objects")
        .willRespondWith()
        .status(204)
        .toPact();
  }

//  @Pact(consumer = "aou-rwb-api", provider = "leonardo")
//  RequestResponsePact stopMissingRuntime(PactDslWithProvider builder) {
//    return builder
//        .given("there is not a runtime in a Google project")
//        .uponReceiving("a request to stop a runtime")
//        .method("POST")
//        .path("/api/google/v1/runtimes/googleProject/runtimename/stop")
//        .willRespondWith()
//        .status(404)
//        .toPact();
//  }

  @Test
  @PactTestFor(pactMethod = "welderLocalize")
  void testWelderLocalize(MockServer mockServer) throws ApiException {
    ApiClient client = new ApiClient();
    client.setBasePath(mockServer.getUrl());
    ProxyApi api = new ProxyApi(client);
    Localize localize= new Localize();
    localize.setAction("frown");
    List<LocalizationEntry> entries = new ArrayList<>();
    LocalizationEntry entry = new LocalizationEntry();
    entry.setSourceUri("a/b/c");
    entry.setLocalDestinationPath("x/y/z");
    entries.add(entry);
    localize.setEntries(entries);

    api.welderLocalize(localize,"googleProject", "runtimename");
  }

//  @Test
//  @PactTestFor(pactMethod = "stopMissingRuntime")
//  void testStopRuntimeWhenRuntimeDoesNotExist(MockServer mockServer) throws ApiException {
//    ApiClient client = new ApiClient();
//    client.setBasePath(mockServer.getUrl());
//    RuntimesApi leoRuntimeService = new RuntimesApi(client);
//
//    assertThrows(
//        Exception.class, () -> leoRuntimeService.stopRuntime("googleProject", "runtimename"));
//  }
}
