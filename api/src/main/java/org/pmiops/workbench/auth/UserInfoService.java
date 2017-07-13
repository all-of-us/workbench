package org.pmiops.workbench.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.model.Userinfoplus;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserInfoService {

  private final HttpTransport httpTransport;
  private final JsonFactory jsonFactory;

  @Autowired
  UserInfoService(HttpTransport httpTransport, JsonFactory jsonFactory) {
    this.httpTransport = httpTransport;
    this.jsonFactory = jsonFactory;
  }

  public Userinfoplus getUserInfo(String token) throws IOException {
    GoogleCredential credential = new GoogleCredential().setAccessToken(token);
    Oauth2 oauth2 = new Oauth2.Builder(httpTransport, jsonFactory, credential).build();
    return oauth2.userinfo().get().execute();
  }
}
