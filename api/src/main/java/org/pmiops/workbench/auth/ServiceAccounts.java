package org.pmiops.workbench.auth;

import com.google.api.client.auth.oauth2.TokenRequest;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.extensions.appengine.http.UrlFetchTransport;
import com.google.api.client.googleapis.auth.oauth2.GoogleOAuthConstants;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.apache.ApacheHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.json.webtoken.JsonWebToken;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.iam.credentials.v1.IamCredentialsClient;
import com.google.cloud.iam.credentials.v1.SignJwtRequest;
import org.pmiops.workbench.config.WorkbenchEnvironment;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

/**
 * Handles functionality related to loading service account credentials and generating derived /
 * impersonated credentials.
 */
@Component
public class ServiceAccounts {

  private static final String SIGN_JWT_URL_FORMAT =
      "https://iamcredentials.googleapis.com/v1/projects/-/serviceAccounts/%s:signJwt";

  /**
   * Retrieves an access token for the Workbench server service account. This should be used
   * carefully, as this account is generally more privileged than an end user researcher account.
   */
  public String workbenchAccessToken(WorkbenchEnvironment workbenchEnvironment, List<String> scopes)
      throws IOException {
    GoogleCredentials scopedCreds = GoogleCredentials.getApplicationDefault()
        .createScoped(scopes);
    scopedCreds.refresh();
    return scopedCreds.getAccessToken().getTokenValue();
  }

  /**
   * Converts a service account Google credential into credentials for impersonating an end user.
   * This method assumes that the given service account has been enabled for domain-wide delegation,
   * and the given set of scopes have been included in the GSuite admin panel.
   *
   * <p>See docs/domain-delegation.md for more details.
   *
   * @param originalCredentials
   * @param userEmail Email address of the user to impersonate.
   * @param scopes The list of Google / OAuth API scopes to be authorized for.
   * @return
   * @throws IOException
   */
  public GoogleCredentials getImpersonatedCredentials(
      GoogleCredentials originalCredentials, String userEmail, List<String> scopes)
      throws IOException {
    GoogleCredentials impersonatedCreds =
        originalCredentials.createScoped(scopes).createDelegated(userEmail);
    impersonatedCreds.refresh();
    return impersonatedCreds;
  }

  public AccessToken getImpersonatedAccessToken(String serviceAccountEmail, String userEmail, List<String> scopes) throws IOException {
    // Step 1: Call the iamcredentials API to generate a signed JWT with the appropriate claims.
    JsonWebToken.Payload payload = createClaims(serviceAccountEmail, userEmail, scopes);
    IamCredentialsClient client = IamCredentialsClient.create();
    SignJwtRequest jwtRequest = SignJwtRequest.newBuilder()
        .setName(String.format("projects/-/serviceAccounts/%s", serviceAccountEmail))
        .setPayload(new JacksonFactory().toString(payload))
        .build();
    Logger log = Logger.getLogger("debug");
    log.info("Request: " + jwtRequest.toString());
    String jwt = client.signJwt(jwtRequest).getSignedJwt();

    // Step 2: Exchange the signed JWT for an access token.
    TokenRequest tokenRequest = new TokenRequest(
      new ApacheHttpTransport(),
      new JacksonFactory(),
      new GenericUrl(GoogleOAuthConstants.TOKEN_SERVER_URL),
      "urn:ietf:params:oauth:grant-type:jwt-bearer");
    tokenRequest.put("assertion", jwt);
    TokenResponse tokenResponse = tokenRequest.execute();
    AccessToken token = new AccessToken(
        tokenResponse.getAccessToken(),
        Date.from(Instant.now().plusSeconds(tokenResponse.getExpiresInSeconds())));
    log.info("Token: " + token.toString());
    return token;
  }

  private JsonWebToken.Payload createClaims(String serviceAccountEmail, String userEmail, List<String> scopes) {
    JsonWebToken.Payload payload = new JsonWebToken.Payload();
    payload.setIssuedAtTimeSeconds(Instant.now().getEpochSecond());
    payload.setExpirationTimeSeconds(Instant.now().getEpochSecond() + 3600);
    payload.setAudience(GoogleOAuthConstants.TOKEN_SERVER_URL);
    payload.setIssuer(serviceAccountEmail);
    payload.setSubject(userEmail);
    payload.set("scope", String.join(" ", scopes));
    return payload;
  }
}
