package org.pmiops.workbench.ras;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.MemoryDataStoreFactory;
import java.io.IOException;
import java.util.Set;

/**
 * Wrapper class to make <a href="https://openid.net/specs/openid-connect-core-1_0.html">OpenId
 * Connect call</a> using Google OAuth Library.
 */
public class OpenIdConnectClient {
  private static final NetHttpTransport DEFAULT_HTTP_TRANSPORT = new NetHttpTransport();
  private static final JacksonFactory DEFAULT_JACKSON_FACTORY = new JacksonFactory();

  private final AuthorizationCodeFlow codeFlow;
  private final String userInfoUrl;
  private final ObjectMapper objectMapper;

  OpenIdConnectClient(String clientId, String clientSecret, String tokenUrl, String authorizeUrl,
      String userInfoUrl)
      throws IOException {
    this.userInfoUrl = userInfoUrl;
    this.codeFlow = newAuthCodeFlow(clientId,clientSecret, tokenUrl, authorizeUrl);
    this.objectMapper = new ObjectMapper();
  }

  /** Get access token by code. */
  public TokenResponse codeExchange(
    String code, String redirectUrl, Set<String> scopes)
      throws IOException {
    return codeFlow.newTokenRequest(code)
        .setScopes(scopes)
        .setRedirectUri(redirectUrl)
        .setGrantType("authorization_code")
        .execute();
  }

  /**
   * Get OIDC user info by hitting UserInfo endpoint.
   * See <a href="https://openid.net/specs/openid-connect-core-1_0.html#UserInfo">OIDC UserInfo</a>
   */
  public JsonNode fetchUserInfo(String accessToken) throws IOException {
    HttpResponse response =
        executeGet(
            DEFAULT_HTTP_TRANSPORT,
            accessToken,
            new GenericUrl(userInfoUrl));
    return objectMapper.readTree(response.getContentEncoding());
  }

  /** Helper method to decode a JWT into {@link DecodedJWT} object. */
  static DecodedJWT decodedJwt(String token) {
    return JWT.decode(token);
  }

  /** Helper method to create a {@link AuthorizationCodeFlow}. */
  private static AuthorizationCodeFlow newAuthCodeFlow(
      String clientId, String clientSecret, String tokenUrl, String authorizeUrl)
      throws IOException {
    return new AuthorizationCodeFlow.Builder(
            BearerToken.queryParameterAccessMethod(),
            DEFAULT_HTTP_TRANSPORT,
            DEFAULT_JACKSON_FACTORY,
            new GenericUrl(tokenUrl),
            new BasicAuthentication(clientId, clientSecret),
            clientId,
            authorizeUrl)
        .setCredentialDataStore(StoredCredential.getDefaultDataStore(new MemoryDataStoreFactory()))
        .build();
  }

  private static HttpResponse executeGet(
      HttpTransport transport, String accessToken, GenericUrl url)
      throws IOException {
    Credential credential =
        new Credential(BearerToken.authorizationHeaderAccessMethod()).setAccessToken(accessToken);
    HttpRequestFactory requestFactory = transport.createRequestFactory(credential);
    return requestFactory.buildGetRequest(url).execute();
  }
}
