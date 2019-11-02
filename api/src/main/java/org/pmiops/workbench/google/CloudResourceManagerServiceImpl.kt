package org.pmiops.workbench.google

import com.google.api.client.googleapis.util.Utils.getDefaultJsonFactory

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.HttpTransport
import com.google.api.services.cloudresourcemanager.CloudResourceManager
import com.google.api.services.cloudresourcemanager.CloudResourceManagerScopes
import com.google.api.services.cloudresourcemanager.model.Project
import java.io.IOException
import java.util.Arrays
import javax.inject.Provider
import org.pmiops.workbench.auth.Constants
import org.pmiops.workbench.auth.ServiceAccounts
import org.pmiops.workbench.db.model.User
import org.pmiops.workbench.exceptions.ExceptionUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class CloudResourceManagerServiceImpl @Autowired
constructor(
        @param:Qualifier(Constants.CLOUD_RESOURCE_MANAGER_ADMIN_CREDS)
        private val cloudResourceManagerAdminCredsProvider: Provider<GoogleCredential>,
        private val httpTransport: HttpTransport,
        private val retryHandler: GoogleRetryHandler,
        private val serviceAccounts: ServiceAccounts) : CloudResourceManagerService {

    @Throws(IOException::class)
    private fun getCloudResourceManagerServiceWithImpersonation(user: User): CloudResourceManager {
        // Load credentials for the cloud-resource-manager Service Account. This account has been
        // granted
        // domain-wide delegation for the OAuth scopes required by cloud apis.
        var googleCredential = cloudResourceManagerAdminCredsProvider.get()

        googleCredential = serviceAccounts.getImpersonatedCredential(googleCredential, user.email, SCOPES)

        return CloudResourceManager.Builder(
                httpTransport, getDefaultJsonFactory(), googleCredential)
                .setApplicationName(APPLICATION_NAME)
                .build()
    }

    override fun getAllProjectsForUser(user: User): List<Project> {
        try {
            return retryHandler.runAndThrowChecked { context ->
                getCloudResourceManagerServiceWithImpersonation(user)
                        .projects()
                        .list()
                        .execute()
                        .projects
            }
        } catch (e: IOException) {
            throw ExceptionUtils.convertGoogleIOException(e)
        }

    }

    companion object {
        private val APPLICATION_NAME = "All of Us Researcher Workbench"

        val SCOPES = Arrays.asList(CloudResourceManagerScopes.CLOUD_PLATFORM_READ_ONLY)
    }
}
