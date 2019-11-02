package org.pmiops.workbench.auth

object Constants {
    /**
     * Bean names for the various types of GoogleCredential dependencies that may be injected into
     * workbench classes. These should be used in bean definitions (in Config classes) and
     * in @Qualifier annotations (in service and component classes).s
     */
    val CLOUD_RESOURCE_MANAGER_ADMIN_CREDS = "cloudResourceManagerAdminCredentials"

    val FIRECLOUD_ADMIN_CREDS = "firecloudAdminCredentials"

    val GSUITE_ADMIN_CREDS = "gsuiteAdminCredentials"
    val DEFAULT_SERVICE_ACCOUNT_CREDS = "defaultServiceAccountCredentials"
}
