package org.pmiops.workbench.billing

import org.pmiops.workbench.billing.BillingProjectBufferStatus.*

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
import org.pmiops.workbench.exceptions.WorkbenchException
import org.pmiops.workbench.firecloud.FireCloudService
import org.pmiops.workbench.firecloud.model.BillingProjectStatus.CreationStatusEnum as FC_BP_STATUS
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.function.Supplier

@Service
class BillingProjectBufferService @Autowired
constructor(
        private val billingProjectBufferEntryDao: BillingProjectBufferEntryDao,
        private val clock: Clock,
        private val fireCloudService: FireCloudService,
        private val workbenchConfigProvider: Provider<WorkbenchConfig>) {

    companion object {

        private val log = Logger.getLogger(BillingProjectBufferService::class.java.name)

        private const val SYNCS_PER_INVOCATION = 5
        private const val PROJECT_BILLING_ID_SIZE = 8
        private const val CREATING_TIMEOUT_MINUTES = 60
        private const val ASSIGNING_TIMEOUT_MINUTES = 10
    }

    fun bufferBillingProject() {
        if (unfilledBufferSpace() <= 0) {
            return
        }

        val projectName = createBillingProjectName()

        fireCloudService.createAllOfUsBillingProject(projectName)
        val entry = BillingProjectBufferEntry()
        entry.fireCloudProjectName = projectName
        entry.creationTime = currentTimestamp()
        entry.setStatusEnum(CREATING, Supplier { currentTimestamp() })

        billingProjectBufferEntryDao.save(entry)
    }

    fun syncBillingProjectStatus() {
        repeat(SYNCS_PER_INVOCATION) {
            val entry = billingProjectBufferEntryDao.findFirstByStatusOrderByLastSyncRequestTimeAsc(
                    StorageEnums.billingProjectBufferStatusToStorage(CREATING)) ?: return

            entry.lastSyncRequestTime = Timestamp(clock.instant().toEpochMilli())

            var status = try { fireCloudService
                    .getBillingProjectStatus(entry.fireCloudProjectName)
                    .creationStatus
            } catch (e: WorkbenchException) {
                log.log(Level.WARNING, "Get BillingProject status call failed", e)
                return
            }

            when (status) {
                FC_BP_STATUS.READY -> entry.setStatusEnum(AVAILABLE, Supplier { currentTimestamp() })
                FC_BP_STATUS.ERROR -> {
                    log.warning("SyncBillingProjectStatus: BillingProject ${entry.fireCloudProjectName} creation failed")
                    entry.setStatusEnum(ERROR, Supplier { currentTimestamp() })
                }
            }


            billingProjectBufferEntryDao.save(entry)
        }
    }

    private fun beforeNow(amt: Long, unit: ChronoUnit): Timestamp {
        return Timestamp(clock.instant().minus(amt, unit).toEpochMilli())
    }

    fun cleanBillingBuffer() {
        billingProjectBufferEntryDao
                .findAllByStatusAndLastStatusChangedTimeLessThan(
                        StorageEnums.billingProjectBufferStatusToStorage(CREATING)!!,
                        beforeNow(CREATING_TIMEOUT_MINUTES.toLong(), ChronoUnit.MINUTES))
        .union(
                billingProjectBufferEntryDao.findAllByStatusAndLastStatusChangedTimeLessThan(
                        StorageEnums.billingProjectBufferStatusToStorage(ASSIGNING)!!,
                        beforeNow(ASSIGNING_TIMEOUT_MINUTES.toLong(), ChronoUnit.MINUTES))
        ).forEach { entry ->
            entry.setStatusEnum(ERROR, Supplier { currentTimestamp() })
            billingProjectBufferEntryDao.save(entry)
        }
    }

    fun assignBillingProject(user: User): BillingProjectBufferEntry {
        val entry = consumeBufferEntryForAssignment()

        fireCloudService.addUserToBillingProject(user.email, entry.fireCloudProjectName)
        entry.setStatusEnum(ASSIGNED, Supplier { currentTimestamp() })
        entry.assignedUser = user
        billingProjectBufferEntryDao.save(entry)

        return entry
    }

    private fun consumeBufferEntryForAssignment(): BillingProjectBufferEntry {
        // Each call to acquire the lock will timeout in 1s if it is currently held
        while (billingProjectBufferEntryDao.acquireAssigningLock() != 1) { }

        val entry: BillingProjectBufferEntry?
        try {
            entry = billingProjectBufferEntryDao.findFirstByStatusOrderByCreationTimeAsc(
                    StorageEnums.billingProjectBufferStatusToStorage(AVAILABLE))

            if (entry == null) {
                log.log(Level.SEVERE, "Consume Buffer call made while Billing Project Buffer was empty")
                throw EmptyBufferException()
            }

            entry.setStatusEnum(ASSIGNING, Supplier { currentTimestamp() })
            billingProjectBufferEntryDao.save(entry)
        } finally {
            billingProjectBufferEntryDao.releaseAssigningLock()
        }

        return entry!!
    }

    private fun createBillingProjectName(): String {
        val randomString = Hashing.sha256()
                .hashUnencodedChars(UUID.randomUUID().toString())
                .toString()
                .substring(0, PROJECT_BILLING_ID_SIZE)
        val prefix = workbenchConfigProvider.get().firecloud.billingProjectPrefix.removeSuffix("-") + "-"
        return prefix + randomString
    }

    private fun unfilledBufferSpace(): Int {
        return bufferMaxCapacity() - currentBufferSize().toInt()
    }

    private fun currentBufferSize(): Long {
        return billingProjectBufferEntryDao.currentBufferSize!!
    }

    private fun bufferMaxCapacity(): Int {
        return workbenchConfigProvider.get().firecloud.billingProjectBufferCapacity
    }

    private fun currentTimestamp(): Timestamp {
        return Timestamp(clock.instant().toEpochMilli())
    }

}
