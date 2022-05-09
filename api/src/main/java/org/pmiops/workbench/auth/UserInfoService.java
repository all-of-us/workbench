package org.pmiops.workbench.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.model.Userinfo;
import org.pmiops.workbench.google.GoogleRetryHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserInfoService {

  private static final String APPLICATION_NAME = "AllOfUs Workbench";

  private final HttpTransport httpTransport;
  private final JsonFactory jsonFactory;
  private final GoogleRetryHandler retryHandler;

  @Autowired
  UserInfoService(
      HttpTransport httpTransport, JsonFactory jsonFactory, GoogleRetryHandler retryHandler) {
    this.httpTransport = httpTransport;
    this.jsonFactory = jsonFactory;
    this.retryHandler = retryHandler;
  }

  public Userinfo getUserInfo(String token) {
    GoogleCredential credential = new GoogleCredential().setAccessToken(token);
    Oauth2 oauth2 =
        new Oauth2.Builder(httpTransport, jsonFactory, credential)
            .setApplicationName(APPLICATION_NAME)
            .build();
    return retryHandler.run((context) -> oauth2.userinfo().get().execute());
  }
}
