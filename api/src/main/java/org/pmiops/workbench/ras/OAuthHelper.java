package org.pmiops.workbench.ras;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.http.BasicAuthentication;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.MemoryDataStoreFactory;
import java.io.IOException;
import java.util.Set;

/** OAuth Helper using Google OAuth Library. */
public class OAuthHelper {
  private static final NetHttpTransport DEFAULT_HTTP_TRANSPORT = new NetHttpTransport();
  private static final JacksonFactory DEFAULT_JACKSON_FACTORY = new JacksonFactory();

  /** Get access token by code. */
  public static TokenResponse codeExchange(AuthorizationCodeFlow flow, String code, String redirectUrl, Set<String> scopes) throws Exception {
    return flow.newTokenRequest(code)
        .setScopes(scopes).setRedirectUri(redirectUrl)
        .setGrantType("authorization_code").execute();
  }

  public static DecodedJWT decodedJwt(String token) {
    return JWT.decode(token);
  }

  public static String fetchUserInfo(String accessToken, GenericUrl url) throws Exception {
    HttpResponse response = executeGet(DEFAULT_HTTP_TRANSPORT, DEFAULT_JACKSON_FACTORY, accessToken, url);
    return response.getContentEncoding();
  }

  /** Helper class to */
  static AuthorizationCodeFlow newAuthCodeFlow(String clientId, String clientSecret, String tokenUrl,
      String authorizeUrl)
      throws IOException {
    return new AuthorizationCodeFlow.Builder(BearerToken.queryParameterAccessMethod(),
        DEFAULT_HTTP_TRANSPORT,
        DEFAULT_JACKSON_FACTORY,
        new GenericUrl(tokenUrl),
        new BasicAuthentication(clientId, clientSecret),
        clientId,
        authorizeUrl)
        .setCredentialDataStore(
            StoredCredential.getDefaultDataStore(
                new MemoryDataStoreFactory()))
        .build();
  }

  private static HttpResponse executeGet(
      HttpTransport transport, JsonFactory jsonFactory, String accessToken, GenericUrl url)
      throws IOException {
    Credential credential =
        new Credential(BearerToken.authorizationHeaderAccessMethod()).setAccessToken(accessToken);
    HttpRequestFactory requestFactory = transport.createRequestFactory(credential);
    return requestFactory.buildGetRequest(url).execute();
  }
}
