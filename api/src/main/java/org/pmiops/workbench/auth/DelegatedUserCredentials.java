package org.pmiops.workbench.auth;

import com.google.api.client.auth.oauth2.TokenRequest;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleOAuthConstants;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.json.webtoken.JsonWebToken;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.OAuth2Credentials;
import com.google.cloud.iam.credentials.v1.IamCredentialsClient;
import com.google.cloud.iam.credentials.v1.SignJwtRequest;
import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * OAuth2 Credentials representing a Service Account using domain-wide delegation of authority to
 * generate access tokens on behalf of a G Suite user.
 *
 * <p>This class calls the IAM Credentials API to request a JWT to be signed using a service
 * account's system-managed private key. This is different from the approach adopted by the
 * ServiceAccountCredentials class, where an application-provided private key is used to self-sign
 * the JWT and then exchange it for an access token.
 *
 * <p>This use of the IAM Credentials API allows a system to use domain-wide delegation of authority
 * to authorize calls as end users without loading private keys directly into the application.
 *
 * <p>This class shares some patterns in common with the ImpersonatedCredentials class; namely, it
 * uses the IAM Credentials API to allow one service account to perform some actions on behalf of
 * another service account. However, this class differs in two notable ways: (1) it supports
 * impersonation of end users, while ImpersonatedCredentials supports only impersonation of service
 * accounts, and (2) it relies on application default credentials for simplicity in the All of Us
 * Researcher Workbench use case.
 *
 * <p>Example usage, for authorizing user requests to the Google Directory API:<br>
 *
 * <pre>
 *   DelegatedUserCredentials delegatedCredentials = new DelegatedUserCredentials(
 *     "service-account-with-dwd-enabled@project-name.iam.gserviceaccount.com",
 *     "admin-gsuite-user@my-gsuite-domain.com",
 *     DirectoryScopes.ADMIN_DIRECTORY_USERS);
 *   Directory service = new Directory.Builder(new NetHttpTransport(), new JacksonFactory(), null)
 *     .setHttpRequestInitializer(new HttpCredentialsAdapter(delegatedCredentials))
 *     .build();
 * </pre>
 */
public class DelegatedUserCredentials extends OAuth2Credentials {

  static final String JWT_BEARER_GRANT_TYPE = "urn:ietf:params:oauth:grant-type:jwt-bearer";
  static final String SERVICE_ACCOUNT_NAME_FORMAT = "projects/-/serviceAccounts/%s";
  static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
  static final Duration ACCESS_TOKEN_DURATION = Duration.ofMinutes(60);

  // The email of the service account whose system-managed key should be used to sign the JWT
  // assertion which is exchanged for an access token. This service account:
  // - Must have domain-wide delegation enabled for the target user's G Suite domain and scopes.
  // - Does not need to be the same service account (SA) as the application default credentials
  // (ADC)
  //     service account. If they are different, the ADC account must have the Service Account Token
  //     Creator role granted on this service account. See
  //     https://cloud.google.com/iam/docs/creating-short-lived-service-account-credentials for more
  //     details.
  private String serviceAccountEmail;
  // The full G Suite email address of the user for whom an access token will be generated.
  private String userEmail;
  // The set of Google OAuth scopes to be requested.
  private List<String> scopes;
  // The HttpTransport to be used for making requests to Google's OAuth2 token server. If null,
  // a default NetHttpTransport instance is used.
  private HttpTransport httpTransport;
  // The IAM Credentials API client to be used for fetching a signed JWT from Google. If null,
  // a default API client will be used.
  private IamCredentialsClient client;

  public DelegatedUserCredentials(
      String serviceAccountEmail, String userEmail, List<String> scopes) {
    super();
    this.serviceAccountEmail = serviceAccountEmail;
    this.userEmail = userEmail;
    this.scopes = scopes;

    if (this.scopes == null) {
      this.scopes = new ArrayList<>();
    }
  }

  @VisibleForTesting
  public void setIamCredentialsClient(IamCredentialsClient client) {
    this.client = client;
  }

  @VisibleForTesting
  public void setHttpTransport(HttpTransport httpTransport) {
    this.httpTransport = httpTransport;
  }

  /**
   * Creates the set of JWT claims representing a service account `serviceAccountEmail` using
   * domain-wide delegation of authority to generate an access token on behalf of a G Suite user,
   * `userEmail`.
   *
   * <p>For reference, see the ServiceAccountCredentials.createAssertion method which builds a
   * similar JWT payload in the context of a JWT being self-signed using a service account's private
   * key.
   *
   * @return
   */
  @VisibleForTesting
  public JsonWebToken.Payload createClaims() {
    JsonWebToken.Payload payload = new JsonWebToken.Payload();
    payload.setIssuedAtTimeSeconds(Instant.now().getEpochSecond());
    payload.setExpirationTimeSeconds(
        Instant.now().getEpochSecond() + ACCESS_TOKEN_DURATION.getSeconds());
    payload.setAudience(GoogleOAuthConstants.TOKEN_SERVER_URL);
    payload.setIssuer(this.serviceAccountEmail);
    payload.setSubject(this.userEmail);
    payload.set("scope", String.join(" ", this.scopes));
    return payload;
  }

  @Override
  public AccessToken refreshAccessToken() throws IOException {
    // The first step is to call the IamCredentials API to generate a signed JWT with the
    // appropriate claims. This call is authorized with application default credentials (ADCs). The
    // ADC service account may be different from `serviceAccountEmail` if the ADC account has the
    // roles/iam.serviceAccountTokenCreator role on the `serviceAccountEmail` account.
    JsonWebToken.Payload payload = createClaims();

    if (client == null) {
      client = IamCredentialsClient.create();
    }
    SignJwtRequest jwtRequest =
        SignJwtRequest.newBuilder()
            .setName(String.format(SERVICE_ACCOUNT_NAME_FORMAT, serviceAccountEmail))
            .setPayload(JSON_FACTORY.toString(payload))
            .build();
    String jwt = client.signJwt(jwtRequest).getSignedJwt();

    // With the signed JWT in hand, we call Google's OAuth2 token server to exchange the JWT for
    // an access token.
    if (httpTransport == null) {
      httpTransport = new NetHttpTransport();
    }
    TokenRequest tokenRequest =
        new TokenRequest(
            httpTransport,
            JSON_FACTORY,
            new GenericUrl(GoogleOAuthConstants.TOKEN_SERVER_URL),
            JWT_BEARER_GRANT_TYPE);
    tokenRequest.put("assertion", jwt);
    TokenResponse tokenResponse = tokenRequest.execute();
    return new AccessToken(
        tokenResponse.getAccessToken(),
        Date.from(Instant.now().plusSeconds(tokenResponse.getExpiresInSeconds())));
  }
}
