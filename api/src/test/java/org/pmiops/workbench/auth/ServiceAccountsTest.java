package org.pmiops.workbench.auth;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import java.io.IOException;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class ServiceAccountsTest {

  @Mock private GoogleCredential serviceAccountCredential;
  @Mock private GoogleCredential impersonatedCredential;
  private GoogleCredential.Builder credentialBuilder;

  private ServiceAccounts serviceAccounts;

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Before
  public void setUp() {
    // Partially mock the credential builder: the builder methods should act as normal, but we'll
    // mock out .build() to instead return a pre-baked GoogleCredential mock.
    credentialBuilder = mock(GoogleCredential.Builder.class, Mockito.CALLS_REAL_METHODS);
    // Partially mock ServiceAccounts to allow the mock CredentialBuilder to be set.
    serviceAccounts = mock(ServiceAccounts.class, Mockito.CALLS_REAL_METHODS);
  }

  @Test
  public void testGetImpersonatedCredential() throws IOException {
    doReturn(credentialBuilder).when(serviceAccounts).getCredentialBuilder();
    when(serviceAccountCredential.getServiceAccountId()).thenReturn("1234567");

    // Avoid trying to actually generate new creds by swapping in a mocked GoogleCredential when
    // the builder is invoked.
    doReturn(impersonatedCredential).when(credentialBuilder).build();
    // Skip the refreshToken process which actually calls OAuth endpoints.
    when(impersonatedCredential.refreshToken()).thenReturn(true);
    // Pretend we retrieved the given access token.
    when(impersonatedCredential.getAccessToken()).thenReturn("impersonated-access-token");

    serviceAccounts.getImpersonatedCredential(
        serviceAccountCredential, "asdf@fake-research-aou.org", Arrays.asList("profile", "email"));

    // The Credential builder should be called with the impersonated username and the original
    // service account's subject ID.
    verify(credentialBuilder).setServiceAccountUser("asdf@fake-research-aou.org");
    verify(credentialBuilder).setServiceAccountId("1234567");
    verify(credentialBuilder).setServiceAccountScopes(Arrays.asList("profile", "email"));

    verify(impersonatedCredential).refreshToken();
  }
}
