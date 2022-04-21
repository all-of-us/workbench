package org.pmiops.workbench.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import java.io.IOException;
import java.io.StringReader;
import org.pmiops.workbench.google.CloudStorageClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

  private static final String CLIENT_SECRET_BUCKET_NAME = "aou_web_client_secret.json";

  private final CloudStorageClient cloudStorageClient;

  @Autowired
  public AuthService(
      CloudStorageClient cloudStorageClient) {
    this.cloudStorageClient = cloudStorageClient;
  }

  public void googleOAuth(String code, String redirectUrl) throws IOException {
    String clientSecret = cloudStorageClient.getCredentialsBucketString(CLIENT_SECRET_BUCKET_NAME);

    // Exchange auth code for access token
    GoogleClientSecrets clientSecrets =
        GoogleClientSecrets.load(GsonFactory.getDefaultInstance(), new StringReader(clientSecret));
    GoogleTokenResponse tokenResponse =
        new GoogleAuthorizationCodeTokenRequest(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                "https://oauth2.googleapis.com/token",
                clientSecrets.getDetails().getClientId(),
                clientSecrets.getDetails().getClientSecret(),
                code,
                redirectUrl) // Specify the same redirect URI that you use with your web
            // app. If you don't have a web version of your app, you can
            // specify an empty string.
            .execute();

    String accessToken = tokenResponse.getAccessToken();

    GoogleIdToken idToken = tokenResponse.parseIdToken();
    GoogleIdToken.Payload payload = idToken.getPayload();
    String userId = payload.getSubject(); // Use this value as a key to identify a user.
    String email = payload.getEmail();
    boolean emailVerified = Boolean.valueOf(payload.getEmailVerified());
    String name = (String) payload.get("name");
    String pictureUrl = (String) payload.get("picture");
    String locale = (String) payload.get("locale");
    String familyName = (String) payload.get("family_name");
    String givenName = (String) payload.get("given_name");

    System.out.println("~~~~~~~ID Token!!!");
    System.out.println("~~~~~~~ID Token!!!");
    System.out.println(idToken);
    System.out.println("~~~~~~~ID Token!!!2222");
    System.out.println(idToken.getPayload());
    System.out.println(idToken.getPayload().toString());
  }
}
