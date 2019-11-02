package org.pmiops.workbench.db.dao

import org.pmiops.workbench.db.model.UserDataUseAgreement
import org.springframework.data.repository.CrudRepository

interface UserDataUseAgreementDao : CrudRepository<UserDataUseAgreement, Long> {
    fun findByUserIdOrderByCompletionTimeDesc(userId: Long): List<UserDataUseAgreement>
}
