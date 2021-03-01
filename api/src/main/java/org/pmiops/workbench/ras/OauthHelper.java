package org.pmiops.workbench.ras;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import java.io.IOException;

public class OauthHelper {
  private static final MediaType X_WWW_FORM_URLENCODED = MediaType.parse("application/x-www-form-urlencoded");
  private String tokenUrl;

  /**
   * Constructor.
   *
   * @param tokenUrl the token URL for Ping Federate.
   */
  public OauthHelper(final String tokenUrl) {
    this.tokenUrl = tokenUrl;
  }

  /**
   * Returns the token URL.
   *
   * @return the token URL.
   */
  public String getTokenUrl() {
    return tokenUrl;
  }

  public String swapAuthenticationCode1(String code) throws IOException {
    return swapAuthenticationCode("e5c5d714-d597-48c8-b564-a249d729d0c9",
        "SECRET",
   "ga4gh_passport_v1" , code);
  }

  /**
   * Swaps the authentication code for an OpenID Connect token.
   *
   * @param clientId                  the client ID.
   * @param clientSecret              the client secret.
   * @param scope                     the list of scopes requested.
   * @param authorizationCode         the authentication code returned by the server.
   * @return                          returns a string of the entire set of tokens.
   * @throws IOException              Signals that an I/O exception of some sort has occurred.
   */
  public String swapAuthenticationCode(final String clientId, final String clientSecret,
      final String scope, final String authorizationCode)
      throws IOException {

    String token = null;
    String redirectUri = "http%3A%2F%2Flocalhost%3A4200%2Fras";
    String redirectUri1 = "http://localhost:4200/ras";

    final String basicAuthString = clientId + ":" + clientSecret;
    final RequestBody body = RequestBody.create(X_WWW_FORM_URLENCODED, String.format(
        "grant_type=authorization_code&redirect_uri=%s&code=%s&scope=%s",
        authorizationCode, redirectUri1, scope));

    // Don't ever do this in production because it allows any certificate!
    final OkHttpClient client = new OkHttpClient();

    final Request request = new Request.Builder()
        .url(tokenUrl)
        .post(body)
        .addHeader("Content-Type", "application/x-www-form-urlencoded")
        .build();

    final Response response = client.newCall(request).execute();

    if (response.isSuccessful()) {
      token = response.body().string();
    }

    return token;
  }
}
