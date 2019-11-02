package org.pmiops.workbench.billing

import org.pmiops.workbench.api.OfflineBillingApiDelegate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class OfflineBillingController @Autowired
internal constructor(
        private val freeTierBillingService: FreeTierBillingService,
        private val billingProjectBufferService: BillingProjectBufferService,
        private val billingGarbageCollectionService: BillingGarbageCollectionService) : OfflineBillingApiDelegate {

    fun billingProjectGarbageCollection(): ResponseEntity<Void> {
        billingGarbageCollectionService.deletedWorkspaceGarbageCollection()
        return ResponseEntity.noContent().build()
    }

    fun bufferBillingProjects(): ResponseEntity<Void> {
        billingProjectBufferService.bufferBillingProjects()
        return ResponseEntity.noContent().build()
    }

    fun syncBillingProjectStatus(): ResponseEntity<Void> {
        billingProjectBufferService.syncBillingProjectStatus()
        return ResponseEntity.noContent().build()
    }

    fun cleanBillingBuffer(): ResponseEntity<Void> {
        billingProjectBufferService.cleanBillingBuffer()
        return ResponseEntity.noContent().build()
    }

    fun checkFreeTierBillingUsage(): ResponseEntity<Void> {
        freeTierBillingService.checkFreeTierBillingUsage()
        return ResponseEntity.noContent().build()
    }
}
