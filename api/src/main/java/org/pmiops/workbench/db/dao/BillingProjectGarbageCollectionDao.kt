package org.pmiops.workbench.db.dao

import org.pmiops.workbench.db.model.BillingProjectGarbageCollection
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface BillingProjectGarbageCollectionDao : CrudRepository<BillingProjectGarbageCollection, Long> {
    fun countAllByOwner(owner: String): Long?
}
