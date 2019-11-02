package org.pmiops.workbench.auth

import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import java.io.IOException
import java.util.Arrays
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule

class ServiceAccountsTest {

    @Mock
    private val serviceAccountCredential: GoogleCredential? = null
    @Mock
    private val impersonatedCredential: GoogleCredential? = null
    private var credentialBuilder: GoogleCredential.Builder? = null

    private var serviceAccounts: ServiceAccounts? = null

    @Rule
    var mockitoRule = MockitoJUnit.rule()

    @Before
    fun setUp() {
        // Partially mock the credential builder: the builder methods should act as normal, but we'll
        // mock out .build() to instead return a pre-baked GoogleCredential mock.
        credentialBuilder = mock(GoogleCredential.Builder::class.java, Mockito.CALLS_REAL_METHODS)
        // Partially mock ServiceAccounts to allow the mock CredentialBuilder to be set.
        serviceAccounts = mock(ServiceAccounts::class.java, Mockito.CALLS_REAL_METHODS)
    }

    @Test
    @Throws(IOException::class)
    fun testGetImpersonatedCredential() {
        doReturn(credentialBuilder).`when`<ServiceAccounts>(serviceAccounts).credentialBuilder
        `when`(serviceAccountCredential!!.serviceAccountId).thenReturn("1234567")

        // Avoid trying to actually generate new creds by swapping in a mocked GoogleCredential when
        // the builder is invoked.
        doReturn(impersonatedCredential).`when`<Builder>(credentialBuilder).build()
        // Skip the refreshToken process which actually calls OAuth endpoints.
        `when`(impersonatedCredential!!.refreshToken()).thenReturn(true)
        // Pretend we retrieved the given access token.
        `when`(impersonatedCredential.accessToken).thenReturn("impersonated-access-token")

        serviceAccounts!!.getImpersonatedCredential(
                serviceAccountCredential, "asdf@fake-research-aou.org", Arrays.asList("profile", "email"))

        // The Credential builder should be called with the impersonated username and the original
        // service account's subject ID.
        verify<Builder>(credentialBuilder).setServiceAccountUser("asdf@fake-research-aou.org")
        verify<Builder>(credentialBuilder).setServiceAccountId("1234567")
        verify<Builder>(credentialBuilder).setServiceAccountScopes(Arrays.asList("profile", "email"))

        verify(impersonatedCredential).refreshToken()
    }
}
