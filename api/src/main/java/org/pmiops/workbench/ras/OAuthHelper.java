package org.pmiops.workbench.ras;

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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.apache.commons.codec.binary.Base64;

/** OAuth Helper using Google OAuth Library. */
public class OAuthHelper {
  private static final NetHttpTransport DEFAULT_HTTP_TRANSPORT = new NetHttpTransport();
  private static final JacksonFactory DEFAULT_JACKSON_FACTORY = new JacksonFactory();

  /** Helper class to */
  private static AuthorizationCodeFlow newFlow(String clientId, String clientSecret, String tokenUrl,
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

  /** Get access token by code. */
  public static TokenResponse codeExchange(AuthorizationCodeFlow flow, String code, String redirectUrl, Set<String> scopes) throws Exception {
    TokenResponse response = flow.newTokenRequest(code)
        .setScopes(scopes).setRedirectUri(redirectUrl)
        .setGrantType("authorization_code").execute();
    System.out.println("~~~~~~~~~~~~~~~!!!!!!!!!!!!");
    System.out.println("ID Token");
    String jwtToken = response.get("id_token").toString();
    String[] split_string = jwtToken.split("\\.");
    String base64EncodedHeader = split_string[0];
    String base64EncodedBody = split_string[1];
    String base64EncodedSignature = split_string[2];

    System.out.println("~~~~~~~~~ JWT Header ~~~~~~~");
    Base64 base64Url = new Base64(true);
    String header = new String(base64Url.decode(base64EncodedHeader));
    System.out.println("JWT Header : " + header);


    System.out.println("~~~~~~~~~ JWT Body ~~~~~~~");
    String body = new String(base64Url.decode(base64EncodedBody));
    System.out.println("JWT Body : "+body);
    System.out.println("JWT Body : "+body);
//    byte[] decodedBytes = Base64.getDecoder().decode(response.get("id_token"));
//    String decodedString = new String(decodedBytes);
    System.out.println(response.get("id_token"));
    System.out.println("Refresh Token");
    System.out.println(response.getRefreshToken());

    // Uncomment if we want to persist a users's credential.
    // Credential credential = flow.createAndStoreCredential(response, "userId");
    return response;
  }

  public static String fetchUserInfo(String accessToken, GenericUrl url) throws Exception {
    HttpResponse response = executeGet(DEFAULT_HTTP_TRANSPORT, DEFAULT_JACKSON_FACTORY, accessToken, url);
    InputStream is = response.getContent();

    StringBuilder sb = new StringBuilder();
    try (Reader reader = new BufferedReader(new InputStreamReader
        (is, Charset.forName(StandardCharsets.UTF_8.name())))) {
      int c = 0;
      while ((c = reader.read()) != -1) {
        sb.append((char) c);
      }
    }
    return response.getContentEncoding();
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
