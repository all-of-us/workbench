package org.pmiops.workbench.auth

import com.google.api.client.googleapis.util.Utils.getDefaultJsonFactory

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.HttpTransport
import com.google.appengine.api.appidentity.AppIdentityService
import com.google.appengine.api.appidentity.AppIdentityServiceFactory
import java.io.IOException
import org.pmiops.workbench.config.WorkbenchEnvironment
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * Handles functionality related to loading service account credentials and generating derived /
 * impersonated credentials.
 */
@Component
class ServiceAccounts @Autowired
constructor(private val httpTransport: HttpTransport) {

    val credentialBuilder: GoogleCredential.Builder
        get() = GoogleCredential.Builder()

    /**
     * Retrieves an access token for the Workbench server service account. This should be used
     * carefully, as this account is generally more privileged than an end user researcher account.
     */
    @Throws(IOException::class)
    fun workbenchAccessToken(workbenchEnvironment: WorkbenchEnvironment, scopes: List<String>): String {
        // When running locally, we get application default credentials in a different way than
        // when running in Cloud.
        if (workbenchEnvironment.isDevelopment) {
            val credential = GoogleCredential.getApplicationDefault().createScoped(scopes)
            credential.refreshToken()
            return credential.accessToken
        }
        val appIdentity = AppIdentityServiceFactory.getAppIdentityService()
        val accessTokenResult = appIdentity.getAccessToken(scopes)
        return accessTokenResult.accessToken
    }

    /**
     * Converts a service account Google credential into credentials for impersonating an end user.
     * This method assumes that the given service account has been enabled for domain-wide delegation,
     * and the given set of scopes have been included in the GSuite admin panel.
     *
     *
     * See docs/domain-delegation.md for more details.
     *
     * @param serviceAccountCredential
     * @param userEmail Email address of the user to impersonate.
     * @param scopes The list of Google / OAuth API scopes to be authorized for.
     * @return
     * @throws IOException
     */
    @Throws(IOException::class)
    fun getImpersonatedCredential(
            serviceAccountCredential: GoogleCredential, userEmail: String, scopes: List<String>): GoogleCredential {
        // Build derived credentials for impersonating the target user.
        val impersonatedUserCredential = credentialBuilder
                .setJsonFactory(getDefaultJsonFactory())
                .setTransport(httpTransport)
                .setServiceAccountUser(userEmail)
                .setServiceAccountId(serviceAccountCredential.serviceAccountId)
                .setServiceAccountScopes(scopes)
                .setServiceAccountPrivateKey(serviceAccountCredential.serviceAccountPrivateKey)
                .setServiceAccountPrivateKeyId(serviceAccountCredential.serviceAccountPrivateKeyId)
                .setTokenServerEncodedUrl(serviceAccountCredential.tokenServerEncodedUrl)
                .build()

        // Initiate the OAuth flow to populate the access token.
        impersonatedUserCredential.refreshToken()
        return impersonatedUserCredential
    }
}
