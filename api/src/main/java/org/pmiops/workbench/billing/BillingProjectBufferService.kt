package org.pmiops.workbench.billing

import org.pmiops.workbench.db.model.BillingProjectBufferEntry.BillingProjectBufferStatus.ASSIGNED
import org.pmiops.workbench.db.model.BillingProjectBufferEntry.BillingProjectBufferStatus.ASSIGNING
import org.pmiops.workbench.db.model.BillingProjectBufferEntry.BillingProjectBufferStatus.AVAILABLE
import org.pmiops.workbench.db.model.BillingProjectBufferEntry.BillingProjectBufferStatus.CREATING
import org.pmiops.workbench.db.model.BillingProjectBufferEntry.BillingProjectBufferStatus.ERROR

import com.google.common.collect.Iterables
import com.google.common.hash.Hashing
import java.sql.Timestamp
import java.time.Clock
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.logging.Level
import java.util.logging.Logger
import javax.inject.Provider
import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.db.dao.BillingProjectBufferEntryDao
import org.pmiops.workbench.db.model.BillingProjectBufferEntry
import org.pmiops.workbench.db.model.StorageEnums
import org.pmiops.workbench.db.model.User
import org.pmiops.workbench.exceptions.NotFoundException
import org.pmiops.workbench.exceptions.WorkbenchException
import org.pmiops.workbench.firecloud.FireCloudService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class BillingProjectBufferService @Autowired
constructor(
        private val billingProjectBufferEntryDao: BillingProjectBufferEntryDao,
        private val clock: Clock,
        private val fireCloudService: FireCloudService,
        private val workbenchConfigProvider: Provider<WorkbenchConfig>) {

    private val currentTimestamp: Timestamp
        get() = Timestamp(clock.instant().toEpochMilli())

    private val unfilledBufferSpace: Int
        get() = bufferMaxCapacity - currentBufferSize.toInt()

    private val currentBufferSize: Long
        get() = billingProjectBufferEntryDao.currentBufferSize!!

    private val bufferMaxCapacity: Int
        get() = workbenchConfigProvider.get().billing.bufferCapacity

    /** Makes a configurable number of project creation attempts.  */
    fun bufferBillingProjects() {
        val creationAttempts = this.workbenchConfigProvider.get().billing.bufferRefillProjectsPerTask
        for (i in 0 until creationAttempts) {
            bufferBillingProject()
        }
    }

    /**
     * Creates a new billing project in the buffer, and kicks off the FireCloud project creation.
     *
     *
     * No action is taken if the buffer is full.
     */
    private fun bufferBillingProject() {
        if (unfilledBufferSpace <= 0) {
            return
        }

        val projectName = createBillingProjectName()

        val entry = BillingProjectBufferEntry()
        entry.fireCloudProjectName = projectName
        entry.creationTime = Timestamp(clock.instant().toEpochMilli())
        entry.setStatusEnum(CREATING, Supplier<Timestamp> { this.getCurrentTimestamp() })
        billingProjectBufferEntryDao.save(entry)

        fireCloudService.createAllOfUsBillingProject(projectName)
    }

    fun syncBillingProjectStatus() {
        for (i in 0 until SYNCS_PER_INVOCATION) {
            val entry = billingProjectBufferEntryDao.findFirstByStatusOrderByLastSyncRequestTimeAsc(
                    StorageEnums.billingProjectBufferStatusToStorage(CREATING)!!)
                    ?: return

            entry.lastSyncRequestTime = Timestamp(clock.instant().toEpochMilli())

            try {
                when (fireCloudService
                        .getBillingProjectStatus(entry.fireCloudProjectName)
                        .getCreationStatus()) {
                    READY -> entry.setStatusEnum(AVAILABLE, Supplier<Timestamp> { this.getCurrentTimestamp() })
                    ERROR -> {
                        log.warning(
                                String.format(
                                        "SyncBillingProjectStatus: BillingProject %s creation failed",
                                        entry.fireCloudProjectName))
                        entry.setStatusEnum(ERROR, Supplier<Timestamp> { this.getCurrentTimestamp() })
                    }
                    CREATING, ADDINGTOPERIMETER -> {
                    }
                    else -> {
                    }
                }
            } catch (e: NotFoundException) {
                log.log(
                        Level.WARNING,
                        "Get BillingProjectStatus call failed for "
                                + entry.fireCloudProjectName
                                + ". Project not found.",
                        e)
            } catch (e: WorkbenchException) {
                log.log(
                        Level.WARNING,
                        "Get BillingProjectStatus call failed for " + entry.fireCloudProjectName!!,
                        e)
            }

            billingProjectBufferEntryDao.save(entry)
        }
    }

    fun cleanBillingBuffer() {
        Iterables.concat(
                billingProjectBufferEntryDao.findAllByStatusAndLastStatusChangedTimeLessThan(
                        StorageEnums.billingProjectBufferStatusToStorage(CREATING)!!,
                        Timestamp(
                                clock
                                        .instant()
                                        .minus(CREATING_TIMEOUT_MINUTES.toLong(), ChronoUnit.MINUTES)
                                        .toEpochMilli())),
                billingProjectBufferEntryDao.findAllByStatusAndLastStatusChangedTimeLessThan(
                        StorageEnums.billingProjectBufferStatusToStorage(ASSIGNING)!!,
                        Timestamp(
                                clock
                                        .instant()
                                        .minus(ASSIGNING_TIMEOUT_MINUTES.toLong(), ChronoUnit.MINUTES)
                                        .toEpochMilli())))
                .forEach { entry ->
                    log.warning(
                            "CleanBillingBuffer: Setting status of "
                                    + entry.fireCloudProjectName
                                    + " to ERROR from "
                                    + entry.statusEnum)
                    entry.setStatusEnum(ERROR, Supplier<Timestamp> { this.getCurrentTimestamp() })
                    billingProjectBufferEntryDao.save(entry)
                }
    }

    fun assignBillingProject(user: User): BillingProjectBufferEntry {
        val entry = consumeBufferEntryForAssignment()

        fireCloudService.addUserToBillingProject(user.email, entry.fireCloudProjectName)
        entry.setStatusEnum(ASSIGNED, Supplier<Timestamp> { this.getCurrentTimestamp() })
        entry.assignedUser = user
        billingProjectBufferEntryDao.save(entry)

        return entry
    }

    private fun consumeBufferEntryForAssignment(): BillingProjectBufferEntry {
        // Each call to acquire the lock will timeout in 1s if it is currently held
        while (billingProjectBufferEntryDao.acquireAssigningLock() != 1) {
        }

        val entry: BillingProjectBufferEntry?
        try {
            entry = billingProjectBufferEntryDao.findFirstByStatusOrderByCreationTimeAsc(
                    StorageEnums.billingProjectBufferStatusToStorage(AVAILABLE)!!)

            if (entry == null) {
                log.log(Level.SEVERE, "Consume Buffer call made while Billing Project Buffer was empty")
                throw EmptyBufferException()
            }

            entry.setStatusEnum(ASSIGNING, Supplier<Timestamp> { this.getCurrentTimestamp() })
            billingProjectBufferEntryDao.save(entry)
        } finally {
            billingProjectBufferEntryDao.releaseAssigningLock()
        }

        return entry
    }

    private fun createBillingProjectName(): String {
        val randomString = Hashing.sha256()
                .hashUnencodedChars(UUID.randomUUID().toString())
                .toString()
                .substring(0, PROJECT_BILLING_ID_SIZE)

        var prefix = workbenchConfigProvider.get().billing.projectNamePrefix
        if (!prefix.endsWith("-")) {
            prefix = "$prefix-"
        }

        return prefix + randomString
    }

    companion object {

        private val log = Logger.getLogger(BillingProjectBufferService::class.java.name)

        private val SYNCS_PER_INVOCATION = 5
        private val PROJECT_BILLING_ID_SIZE = 8
        private val CREATING_TIMEOUT_MINUTES = 60
        private val ASSIGNING_TIMEOUT_MINUTES = 10
    }
}
