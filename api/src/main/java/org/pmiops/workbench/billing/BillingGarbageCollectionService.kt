package org.pmiops.workbench.billing

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import java.io.IOException
import java.sql.Timestamp
import java.time.Clock
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.logging.Logger
import javax.inject.Provider
import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.config.WorkbenchConfig.BillingConfig
import org.pmiops.workbench.db.dao.BillingProjectBufferEntryDao
import org.pmiops.workbench.db.dao.BillingProjectGarbageCollectionDao
import org.pmiops.workbench.db.model.BillingProjectBufferEntry
import org.pmiops.workbench.db.model.BillingProjectBufferEntry.BillingProjectBufferStatus
import org.pmiops.workbench.db.model.BillingProjectGarbageCollection
import org.pmiops.workbench.exceptions.NotFoundException
import org.pmiops.workbench.exceptions.ServerErrorException
import org.pmiops.workbench.firecloud.FireCloudService
import org.pmiops.workbench.firecloud.FireCloudServiceImpl
import org.pmiops.workbench.google.CloudStorageService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class BillingGarbageCollectionService @Autowired
constructor(
        private val billingProjectGarbageCollectionDao: BillingProjectGarbageCollectionDao,
        private val billingProjectBufferEntryDao: BillingProjectBufferEntryDao,
        private val fireCloudService: FireCloudService,
        private val workbenchConfigProvider: Provider<WorkbenchConfig>,
        private val cloudStorageServiceProvider: Provider<CloudStorageService>,
        private val clock: Clock) {
    private val garbageCollectionSACredentials: LoadingCache<String, GoogleCredential>

    init {

        this.garbageCollectionSACredentials = initializeCredentialCache()
    }

    private fun initializeCredentialCache(): LoadingCache<String, GoogleCredential> {
        return CacheBuilder.newBuilder()
                .expireAfterWrite(24, TimeUnit.HOURS)
                .build(
                        object : CacheLoader<String, GoogleCredential>() {
                            @Throws(IOException::class)
                            override fun load(saEmail: String): GoogleCredential {
                                return cloudStorageServiceProvider
                                        .get()
                                        .getGarbageCollectionServiceAccountCredentials(saEmail)
                                        .createScoped(FireCloudServiceImpl.FIRECLOUD_API_OAUTH_SCOPES)
                            }
                        })
    }

    // Checks whether the AppEngine service account is a member of this FireCloud project
    private fun appSAIsMemberOfProject(billingProject: String): Boolean {
        try {
            fireCloudService.getBillingProjectStatus(billingProject)
            return true
        } catch (e: NotFoundException) {
            return false
        }

    }

    private fun chooseGarbageCollectionSA(): String {
        val config = workbenchConfigProvider.get().billing
        for (sa in config.garbageCollectionUsers) {
            val count = billingProjectGarbageCollectionDao.countAllByOwner(sa)!!
            if (count < config.garbageCollectionUserCapacity) {
                return sa
            }
        }

        val msg = String.format(
                "No available Garbage Collection Service Accounts.  " + "These GCSAs exceed the configured capacity limit of %d: %s",
                config.garbageCollectionUserCapacity, config.garbageCollectionUsers.joinToString(", "))
        throw ServerErrorException(msg)
    }

    private fun recordGarbageCollection(projectName: String, garbageCollectionSA: String) {

        val gc = BillingProjectGarbageCollection()
        gc.fireCloudProjectName = projectName
        gc.owner = garbageCollectionSA
        billingProjectGarbageCollectionDao.save(gc)

        val entry = billingProjectBufferEntryDao.findByFireCloudProjectName(projectName)
        entry.setStatusEnum(
                BillingProjectBufferStatus.GARBAGE_COLLECTED
        ) { Timestamp(clock.instant().toEpochMilli()) }
        billingProjectBufferEntryDao.save(entry)

        log.info(
                String.format(
                        "Project %s has been garbage-collected and is now owned by %s",
                        projectName, garbageCollectionSA))
    }

    private fun transferOwnership(projectName: String) {
        val appEngineSA = workbenchConfigProvider.get().auth.serviceAccountApiUsers[0]

        val garbageCollectionSA = chooseGarbageCollectionSA()
        fireCloudService.addOwnerToBillingProject(garbageCollectionSA, projectName)

        try {
            val gcsaCredential = garbageCollectionSACredentials.get(garbageCollectionSA)

            gcsaCredential.refreshToken()

            fireCloudService.removeOwnerFromBillingProject(
                    projectName, appEngineSA, gcsaCredential.accessToken)
        } catch (e: ExecutionException) {
            val msg = String.format(
                    "Failure retrieving credentials for garbage collection service account %s",
                    garbageCollectionSA)
            throw ServerErrorException(msg, e)
        } catch (e: IOException) {
            val msg = String.format(
                    "Failure removing user %s as owner of project %s. Successfully added new owner %s",
                    appEngineSA, projectName, garbageCollectionSA)
            throw ServerErrorException(msg, e)
        }

        recordGarbageCollection(projectName, garbageCollectionSA)
    }

    internal fun deletedWorkspaceGarbageCollection() {
        billingProjectBufferEntryDao.findBillingProjectsForGarbageCollection().stream()
                .forEach { projectName ->
                    // determine whether this candidate for garbage collection
                    // has already been deleted or transferred by a process other than GC
                    if (appSAIsMemberOfProject(projectName)) {
                        transferOwnership(projectName)
                    } else {
                        // if it's in the DB as garbage-collected we won't try to do it again
                        recordGarbageCollection(projectName, "unknown")
                    }
                }
    }

    companion object {
        private val log = Logger.getLogger(BillingGarbageCollectionService::class.java.name)
    }
}
