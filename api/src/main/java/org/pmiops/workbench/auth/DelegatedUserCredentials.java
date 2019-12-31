package org.pmiops.workbench.auth;

import com.google.api.client.auth.oauth2.TokenRequest;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleOAuthConstants;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.apache.ApacheHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.json.webtoken.JsonWebToken;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.OAuth2Credentials;
import com.google.cloud.iam.credentials.v1.IamCredentialsClient;
import com.google.cloud.iam.credentials.v1.SignJwtRequest;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

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
 * another service account. However, ImpersonatedCredentials (1) does not support the creation of
 * delegated user credentials, and (2) supports an arbitrary "source credential", while this class
 * relies on application default credentials for simplicity.
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
  // assertion which is exchanged for an access token. This service account does not need to be
  // the same as the application default credentials service account; see
  private String serviceAccountEmail;
  private String userEmail;
  private List<String> scopes;
  private List<String> delegates;

  public DelegatedUserCredentials(
      String serviceAccountEmail, String userEmail, List<String> scopes) {
    super();
    this.serviceAccountEmail = serviceAccountEmail;
    this.userEmail = userEmail;
    this.scopes = scopes;

    if (this.delegates == null) {
      this.delegates = new ArrayList<>();
    }
    if (this.scopes == null) {
      this.scopes = new ArrayList<>();
    }
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
  private JsonWebToken.Payload createClaims() {
    JsonWebToken.Payload payload = new JsonWebToken.Payload();
    payload.setIssuedAtTimeSeconds(Instant.now().getEpochSecond());
    // This effectively sets the requested token expiration time.
    payload.setExpirationTimeSeconds(
        Instant.now().getEpochSecond() + ACCESS_TOKEN_DURATION.getSeconds());
    payload.setAudience(GoogleOAuthConstants.TOKEN_SERVER_URL);
    payload.setIssuer(this.serviceAccountEmail);
    payload.setSubject(this.userEmail);
    payload.set("scope", String.join(" ", this.scopes));
    Logger log = Logger.getLogger("asdf");
    log.info("Payload: " + payload);
    return payload;
  }

  @Override
  public AccessToken refreshAccessToken() throws IOException {
    // The first step is to call the IamCredentials API to generate a signed JWT with the
    // appropriate claims. This call is authorized with application default credentials (ADCs). The
    // ADC service account may be different from `serviceAccountEmail` if the ADC account has the
    // roles/iam.serviceAccountTokenCreator role on the `serviceAccountEmail` account.
    JsonWebToken.Payload payload = createClaims();
    IamCredentialsClient client = IamCredentialsClient.create();
    SignJwtRequest jwtRequest =
        SignJwtRequest.newBuilder()
            .setName(String.format(SERVICE_ACCOUNT_NAME_FORMAT, serviceAccountEmail))
            .setPayload(JSON_FACTORY.toString(payload))
            .addAllDelegates(delegates)
            .build();
    String jwt = client.signJwt(jwtRequest).getSignedJwt();

    // With the signed JWT in hand, we call Google's OAuth2 token server to exchange the JWT for
    // an access token.
    TokenRequest tokenRequest =
        new TokenRequest(
            new ApacheHttpTransport(),
            JSON_FACTORY,
            new GenericUrl(GoogleOAuthConstants.TOKEN_SERVER_URL),
            JWT_BEARER_GRANT_TYPE);
    tokenRequest.put("assertion", jwt);
    TokenResponse tokenResponse = tokenRequest.execute();
    AccessToken token =
        new AccessToken(
            tokenResponse.getAccessToken(),
            Date.from(Instant.now().plusSeconds(tokenResponse.getExpiresInSeconds())));
    return token;
  }
}
