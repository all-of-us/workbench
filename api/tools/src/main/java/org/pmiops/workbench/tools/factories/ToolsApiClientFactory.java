package org.pmiops.workbench.tools.factories;

import com.google.cloud.iam.credentials.v1.IamCredentialsClient;
import com.google.protobuf.Duration;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

public class ToolsApiClientFactory {

  protected static final String[] FC_SCOPES =
      new String[] {
        "https://www.googleapis.com/auth/userinfo.profile",
        "https://www.googleapis.com/auth/userinfo.email",
        "https://www.googleapis.com/auth/cloud-billing"
      };

  public static String getAccessToken(String targetServiceAccount) throws IOException {
    try (var client = IamCredentialsClient.create()) {
      return client
          .generateAccessToken(
              "projects/-/serviceAccounts/" + targetServiceAccount,
              Collections.emptyList(),
              Arrays.asList(FC_SCOPES),
              Duration.newBuilder().setSeconds(60 * 60).build())
          .getAccessToken();
    }
  }
}
