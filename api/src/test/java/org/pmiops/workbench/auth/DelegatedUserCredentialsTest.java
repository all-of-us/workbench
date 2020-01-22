package org.pmiops.workbench.auth;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.client.googleapis.auth.oauth2.GoogleOAuthConstants;
import com.google.api.client.googleapis.testing.auth.oauth2.MockTokenServerTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.json.webtoken.JsonWebSignature;
import com.google.api.client.json.webtoken.JsonWebToken;
import com.google.api.client.util.PemReader;
import com.google.api.client.util.SecurityUtils;
import com.google.cloud.iam.credentials.v1.IamCredentialsClient;
import com.google.cloud.iam.credentials.v1.SignJwtRequest;
import com.google.cloud.iam.credentials.v1.SignJwtResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class DelegatedUserCredentialsTest {

  static final String USER_EMAIL = "john.doe@researchallofus.org";
  static final String SERVICE_ACCOUNT_EMAIL = "gsuite-admin@test-project.iam.gserviceaccount.com";
  static final List<String> SCOPES = Arrays.asList("openid", "profile");
  static final String MOCK_ACCESS_TOKEN = "access-token";

  static final String SA_PRIVATE_KEY_ID = "private-key-for-testing-only";
  // A random private key string generated for testing purposed with the following commands:
  // $ openssl genrsa -out keypair.pem 512
  // $ openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in keypair.pem -out pkcs8.key
  static final String SA_PRIVATE_KEY_PKCS8 =
      "-----BEGIN PRIVATE KEY-----\n"
          + "MIIBVAIBADANBgkqhkiG9w0BAQEFAASCAT4wggE6AgEAAkEA04/ClUlp9Y1HEOPe\n"
          + "2FGpSkkSkR94c2JD1Wet2qyVvXLlCBqQyTYtJugSaBWNSDe/M+6astFrWLOSZU2o\n"
          + "OVK1AwIDAQABAkEAwQQX8zvXgEA05iP/3Dwkx7GDTwP3UM4GNV0yMJ/kvcG8lTzh\n"
          + "/WpVThpktn5roeoiwOcQP3jbGbUTlGw2JJYVAQIhAO2jAKAoqoBIiEimrES0eery\n"
          + "8aVmJEu+LzO2+ZgjNRZJAiEA4+juaKu6PxbBHtV4NVN1viX0mIRUxr4jcrZGlz3d\n"
          + "QOsCIFRCxggEI2DVVy2bm93IuKosdq6VJy2MRCRsLthZM4uxAiB0A/HApJJFZT7f\n"
          + "hEkR1C9eoRGWxd4l4UpILZNXj+1eCwIgZOwMPzJi5thQysHlvf0cqBO/7tv2fd6K\n"
          + "qzJzcrNQfMs=\n"
          + "-----END PRIVATE KEY-----";

  // We'll handle mocking out the call to the IAM Credentials API.
  @Mock private IamCredentialsClient mockIamCredentialsClient;
  // Google's API client library provides a convenient mock for their token server API,
  // so we'll use that to mock out the call to request an access token.
  private MockTokenServerTransport mockTokenServerTransport;
  private DelegatedUserCredentials delegatedCredentials;

  @Before
  public void setUp() {
    mockTokenServerTransport = new MockTokenServerTransport();

    delegatedCredentials = new DelegatedUserCredentials(SERVICE_ACCOUNT_EMAIL, USER_EMAIL, SCOPES);
    delegatedCredentials.setHttpTransport(mockTokenServerTransport);
    delegatedCredentials.setIamCredentialsClient(mockIamCredentialsClient);
  }

  /**
   * Creates a PrivateKey from a PKCS8 private key string. This is effectively a copy of
   * ServiceAccountCredentials.privateKeyFromPkcs8 which is privately scoped.
   */
  private static PrivateKey privateKeyFromPkcs8(String privateKeyPem) throws IOException {
    Reader reader = new StringReader(privateKeyPem);
    PemReader.Section section = PemReader.readFirstSectionAndClose(reader, "PRIVATE KEY");
    if (section == null) {
      throw new IOException("Invalid PKCS8 data.");
    }
    byte[] bytes = section.getBase64DecodedBytes();
    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(bytes);
    try {
      KeyFactory keyFactory = SecurityUtils.getRsaKeyFactory();
      PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
      return privateKey;
    } catch (NoSuchAlgorithmException | InvalidKeySpecException exception) {
      throw new IOException("Unexpected expcetion reading PKCS data", exception);
    }
  }

  /**
   * Given a JWT payload, create a self-signed JWT using an OAuth2 compliant algorithm. This method
   * mocks out the behavior of the IAM Credentials API, which signs JWTs using Google-managed
   * service account private keys.
   */
  private static String createSelfSignedJwt(JsonWebToken.Payload payload)
      throws IOException, GeneralSecurityException {
    JsonWebSignature.Header header = new JsonWebSignature.Header();
    header.setAlgorithm("RS256");
    header.setType("JWT");
    header.setKeyId(SA_PRIVATE_KEY_ID);

    return JsonWebSignature.signUsingRsaSha256(
        privateKeyFromPkcs8(SA_PRIVATE_KEY_PKCS8),
        JacksonFactory.getDefaultInstance(),
        header,
        payload);
  }

  @Test
  public void testClaims() {
    JsonWebToken.Payload payload = delegatedCredentials.createJwtPayload();

    assertThat(payload.getAudience()).isEqualTo(GoogleOAuthConstants.TOKEN_SERVER_URL);
    assertThat(payload.getIssuer()).isEqualTo(SERVICE_ACCOUNT_EMAIL);
    assertThat(payload.getSubject()).isEqualTo(USER_EMAIL);
    assertThat(payload.get("scope")).isEqualTo(String.join(" ", SCOPES));
  }

  @Test
  public void testRefreshFlow() throws IOException {
    // Mock out the IAM Credentials API client to create a self-signed JsonWebSignature instead of
    // calling Google's API.
    when(mockIamCredentialsClient.signJwt(any(SignJwtRequest.class)))
        .then(
            invocation -> {
              SignJwtRequest request = invocation.getArgument(0);
              JsonWebToken.Payload payload =
                  DelegatedUserCredentials.JSON_FACTORY.fromInputStream(
                      new ByteArrayInputStream(request.getPayload().getBytes()),
                      JsonWebToken.Payload.class);
              return SignJwtResponse.newBuilder()
                  .setSignedJwt(createSelfSignedJwt(payload))
                  .build();
            });
    // Register the expected service account & access token with the mock token server transport.
    mockTokenServerTransport.addServiceAccount(SERVICE_ACCOUNT_EMAIL, MOCK_ACCESS_TOKEN);

    // Kick off the refresh flow.
    delegatedCredentials.refresh();

    // Verify the call to IAM Credentials API.
    ArgumentCaptor<SignJwtRequest> captor = ArgumentCaptor.forClass(SignJwtRequest.class);
    verify(mockIamCredentialsClient, Mockito.times(1)).signJwt(captor.capture());
    assertThat(captor.getValue().getName())
        .isEqualTo("projects/-/serviceAccounts/" + SERVICE_ACCOUNT_EMAIL);

    // The mockTokenServerTransport class runs some lightweight verification of its own (i.e.,
    // ensuring the signed JWT can be parsed and that the service account is known). Beyond that,
    // we mainly care that the access token is returned and has a correct expiration.
    assertThat(delegatedCredentials.getAccessToken().getTokenValue()).isEqualTo(MOCK_ACCESS_TOKEN);
    assertThat(
            delegatedCredentials.getAccessToken().getExpirationTime().toInstant().getEpochSecond())
        .isEqualTo(Instant.now().getEpochSecond() + 3600);
  }
}
