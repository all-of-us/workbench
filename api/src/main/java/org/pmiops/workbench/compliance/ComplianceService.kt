package org.pmiops.workbench.compliance

import org.pmiops.workbench.moodle.ApiException
import org.pmiops.workbench.moodle.model.BadgeDetails

interface ComplianceService {

    /**
     * Get Moodle Id associated with Aou User email id
     *
     * @param email
     * @return Moodle Id
     * @throws ApiException
     */
    @Throws(ApiException::class)
    fun getMoodleId(email: String): Int?

    /**
     * Get the list of badges earned by User
     *
     * @param userMoodleId
     * @return list of badges/completed training by user
     * @throws ApiException
     */
    @Throws(ApiException::class)
    fun getUserBadge(userMoodleId: Int): List<BadgeDetails>
}
