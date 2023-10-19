package org.pmiops.workbench.absorb;

import javax.annotation.Nullable;

public class Credentials {
  public String apiKey = null;
  public String accessToken = null;
  public String userId = null;

  public Credentials(String apiKey, String accessToken, @Nullable String userId) {
    this.apiKey = apiKey;
    this.accessToken = accessToken;
    this.userId = userId;
  }
}
