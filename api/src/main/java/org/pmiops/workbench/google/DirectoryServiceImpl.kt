/**
 * Note: this file is tested with integration tests rather than unit tests. See
 * src/integration/.../DirectoryServiceImplIntegrationTest.java for test cases.
 */
package org.pmiops.workbench.google

import com.google.api.client.googleapis.util.Utils.getDefaultJsonFactory

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.HttpTransport
import com.google.api.services.directory.Directory
import com.google.api.services.directory.DirectoryScopes
import com.google.api.services.directory.model.User
import com.google.api.services.directory.model.UserEmail
import com.google.api.services.directory.model.UserName
import com.google.common.collect.Lists
import java.io.IOException
import java.security.SecureRandom
import java.util.Arrays
import java.util.Collections
import java.util.HashMap
import java.util.stream.Collectors
import java.util.stream.IntStream
import javax.inject.Provider
import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.exceptions.ExceptionUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

@Service
class DirectoryServiceImpl @Autowired
constructor(
        @param:Qualifier("gsuiteAdminCredentials") private val googleCredentialProvider: Provider<GoogleCredential>,
        private val configProvider: Provider<WorkbenchConfig>,
        private val httpTransport: HttpTransport,
        private val retryHandler: GoogleRetryHandler) : DirectoryService {

    private val googleDirectoryService: Directory
        get() = Directory.Builder(
                httpTransport, getDefaultJsonFactory(), createCredentialWithImpersonation())
                .setApplicationName(APPLICATION_NAME)
                .build()

    private fun createCredentialWithImpersonation(): GoogleCredential {
        val googleCredential = googleCredentialProvider.get()
        val gSuiteDomain = configProvider.get().googleDirectoryService.gSuiteDomain
        return GoogleCredential.Builder()
                .setTransport(httpTransport)
                .setJsonFactory(getDefaultJsonFactory())
                // Must be an admin user in the GSuite domain.
                .setServiceAccountUser("directory-service@$gSuiteDomain")
                .setServiceAccountId(googleCredential.serviceAccountId)
                .setServiceAccountScopes(SCOPES)
                .setServiceAccountPrivateKey(googleCredential.serviceAccountPrivateKey)
                .setServiceAccountPrivateKeyId(googleCredential.serviceAccountPrivateKeyId)
                .setTokenServerEncodedUrl(googleCredential.tokenServerEncodedUrl)
                .build()
    }

    private fun gSuiteDomain(): String {
        return configProvider.get().googleDirectoryService.gSuiteDomain
    }

    fun getUserByUsername(username: String): User? {
        return getUser(username + "@" + gSuiteDomain())
    }

    /**
     * Fetches a user by their GSuite email address.
     *
     *
     * If the user is not found, a null value will be returned (no exception is thrown).
     *
     * @param email
     * @return
     */
    override fun getUser(email: String): User? {
        try {
            // We use the "full" projection to include custom schema fields in the Directory API response.
            return retryHandler.runAndThrowChecked { context -> googleDirectoryService.users().get(email).setProjection("full").execute() }
        } catch (e: GoogleJsonResponseException) {
            // Handle the special case where we're looking for a not found user by returning
            // null.
            if (e.details.code == HttpStatus.NOT_FOUND.value()) {
                return null
            }
            throw ExceptionUtils.convertGoogleIOException(e)
        } catch (e: IOException) {
            throw ExceptionUtils.convertGoogleIOException(e)
        }

    }

    override fun isUsernameTaken(username: String): Boolean {
        return getUserByUsername(username) != null
    }

    // Returns a user's contact email address via the custom schema in the directory API.
    fun getContactEmailAddress(username: String): String {
        return getUserByUsername(username)!!
                .customSchemas[GSUITE_AOU_SCHEMA_NAME][GSUITE_FIELD_CONTACT_EMAIL] as String
    }

    override fun createUser(
            givenName: String, familyName: String, username: String, contactEmail: String): User {
        val primaryEmail = username + "@" + gSuiteDomain()
        val password = randomString()

        val user = User()
                .setPrimaryEmail(primaryEmail)
                .setPassword(password)
                .setName(UserName().setGivenName(givenName).setFamilyName(familyName))
                .setChangePasswordAtNextLogin(true)
                .setOrgUnitPath(GSUITE_WORKBENCH_ORG_UNIT_PATH)
        addCustomSchemaAndEmails(user, primaryEmail, contactEmail)

        retryHandler.run { context -> googleDirectoryService.users().insert(user).execute() }
        return user
    }

    override fun resetUserPassword(email: String): User {
        val user = getUser(email)
        val password = randomString()
        user!!.password = password
        retryHandler.run { context -> googleDirectoryService.users().update(email, user).execute() }
        return user
    }

    override fun deleteUser(username: String) {
        try {
            retryHandler.runAndThrowChecked { context ->
                googleDirectoryService
                        .users()
                        .delete(username + "@" + gSuiteDomain())
                        .execute()
            }
        } catch (e: GoogleJsonResponseException) {
            if (e.details.code == HttpStatus.NOT_FOUND.value()) {
                // Deleting a user that doesn't exist will have no effect.
                return
            }
            throw ExceptionUtils.convertGoogleIOException(e)
        } catch (e: IOException) {
            throw ExceptionUtils.convertGoogleIOException(e)
        }

    }

    private fun randomString(): String {
        return IntStream.range(0, 17)
                .boxed()
                .map { x -> ALLOWED[rnd.nextInt(ALLOWED.length)] }
                .map<String>(Function<Char, String> { it.toString() })
                .collect<String, *>(Collectors.joining(""))
    }

    companion object {

        private val ALLOWED = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()"
        private val APPLICATION_NAME = "All of Us Researcher Workbench"
        // Matches the API org unit path defined in the Gsuite UI where researcher accounts reside.
        private val GSUITE_WORKBENCH_ORG_UNIT_PATH = "/workbench-users"
        // Name of the GSuite custom schema containing AOU custom fields.
        private val GSUITE_AOU_SCHEMA_NAME = "All_of_Us_Workbench"
        // Name of the "contact email" custom field, which is stored within the AoU GSuite custom schema.
        private val GSUITE_FIELD_CONTACT_EMAIL = "Contact_email_address"
        // Name of the "institution" custom field, whose value is the same for all Workbench users.
        private val GSUITE_FIELD_INSTITUTION = "Institution"
        private val INSTITUTION_FIELD_VALUE = "All of Us Research Workbench"

        private val rnd = SecureRandom()

        // This list must exactly match the scopes allowed via the GSuite Domain Admin page here:
        // https://admin.google.com/fake-research-aou.org/AdminHome?chromeless=1#OGX:ManageOauthClients
        // replace 'fake-research-aou.org' with the specific domain that you want to manage
        // For example, ADMIN_DIRECTORY_USER does not encapsulate ADMIN_DIRECTORY_USER_READONLY — it must
        // be explicit.
        // The "Client Name" field in that form must be the client ID of the service account. The field
        // will accept the email address of the service account and lookup the correct client ID giving
        // the impression that the email address is an acceptable substitute, but testing shows that this
        // doesn't actually work.
        private val SCOPES = Arrays.asList(
                DirectoryScopes.ADMIN_DIRECTORY_USER_ALIAS,
                DirectoryScopes.ADMIN_DIRECTORY_USER_ALIAS_READONLY,
                DirectoryScopes.ADMIN_DIRECTORY_USER, DirectoryScopes.ADMIN_DIRECTORY_USER_READONLY)

        fun addCustomSchemaAndEmails(user: User, primaryEmail: String, contactEmail: String?) {
            // GSuite custom fields for Workbench user accounts.
            // See the Moodle integration doc (broad.io/aou-moodle) for more details, as this
            // was primarily set up for Moodle SSO integration.
            val aouCustomFields = HashMap<String, Any>()
            // The value of this field must match one of the allowed values in the Moodle installation.
            // Since this value is unlikely to ever change, we use a hard-coded constant rather than an env
            // variable.
            aouCustomFields[GSUITE_FIELD_INSTITUTION] = INSTITUTION_FIELD_VALUE

            if (contactEmail != null) {
                // This gives us a structured place to store researchers' contact email addresses, in
                // case we want to pass it to other systems (e.g. Zendesk or Moodle) via SAML mapped fields.
                aouCustomFields[GSUITE_FIELD_CONTACT_EMAIL] = contactEmail
            }

            // In addition to the custom schema value, we store each user's contact email as a secondary
            // email address with type "home". This makes it show up nicely in GSuite admin as the
            // user's "Secondary email".
            val emails = Lists.newArrayList(
                    UserEmail().setType("work").setAddress(primaryEmail).setPrimary(true))
            if (contactEmail != null) {
                emails.add(UserEmail().setType("home").setAddress(contactEmail))
            }
            user.setEmails(emails)
                    .setRecoveryEmail(contactEmail).customSchemas = Collections.singletonMap<String, Map<String, Any>>(GSUITE_AOU_SCHEMA_NAME, aouCustomFields)
        }
    }
}
