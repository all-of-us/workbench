package org.pmiops.workbench.consumer;

import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import java.util.Map;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(PactConsumerTestExt.class)
class AppsApiTest {

//  @Pact(consumer = "aou-rwb-api", provider = "leonardo")
//  RequestResponsePact getApp(PactDslWithProvider builder) {
//    return builder
//        .given("there is an app in a Google project")
//        .uponReceiving("a request to get that app")
//        .method("GET")
//        .path("/api/google/v1/apps/googleProject/appname")
//        .willRespondWith()
//        .status(200)
//        .headers(contentTypeJsonHeader)
//        .body(
//            newJsonBody(
//                    body -> {
//                      body.stringType("appName", "sample-cromwell-study");
//                      body.stringType("status", "RUNNING");
//                      body.stringType("diskName", "disk-123");
//                      body.stringType("appType", "CROMWELL");
//                      body.array("errors", errors -> {});
//                      body.object(
//                          "cloudContext", context -> context.stringType("cloudprovider", null));
//                    })
//                .build())
//        .toPact();
//  }
//
//  @Test
//  @PactTestFor(pactMethod = "getApp")
//  void testGetAppWhenAppExists(MockServer mockServer) throws ApiException {
//    ApiClient client = new ApiClient();
//    client.setBasePath(mockServer.getUrl());
//    AppsApi leoAppService = new AppsApi(client);
//
//    LeonardoGetAppResponse expected = new LeonardoGetAppResponse();
//    expected.setAppName("sample-cromwell-study");
//    expected.setErrors(new ArrayList<>());
//    expected.setDiskName("disk-123");
//    expected.setStatus(LeonardoAppStatus.RUNNING);
//    expected.setAppType(LeonardoAppType.CROMWELL);
//    expected.setCloudContext(new LeonardoCloudContext());
//
//    LeonardoGetAppResponse response = leoAppService.getApp("googleProject", "appname");
//
//    assertEquals(expected, response);
//  }

  static Map<String, String> contentTypeJsonHeader = Map.of("Content-Type", "application/json");
}
