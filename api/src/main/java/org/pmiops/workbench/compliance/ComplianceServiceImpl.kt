package org.pmiops.workbench.compliance

import javax.inject.Provider
import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.google.CloudStorageService
import org.pmiops.workbench.moodle.ApiException
import org.pmiops.workbench.moodle.api.MoodleApi
import org.pmiops.workbench.moodle.model.BadgeDetails
import org.pmiops.workbench.moodle.model.MoodleUserResponse
import org.pmiops.workbench.moodle.model.UserBadgeResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

@Service
class ComplianceServiceImpl @Autowired
constructor(
        private val cloudStorageService: CloudStorageService,
        private val configProvider: Provider<WorkbenchConfig>,
        private val moodleApiProvider: Provider<MoodleApi>) : ComplianceService {

    private val api = MoodleApi()

    private val token: String
        get() = this.cloudStorageService.moodleApiKey

    private fun enableMoodleCalls(): Boolean {
        return configProvider.get().moodle.enableMoodleBackend
    }

    /**
     * Returns the Moodle ID corresponding to the given AoU user email address.
     *
     *
     * Returns null if no Moodle user ID was found.
     */
    @Throws(ApiException::class)
    override fun getMoodleId(email: String): Int? {
        if (!enableMoodleCalls()) {
            return null
        }
        val response = moodleApiProvider.get().getMoodleId(token, GET_MOODLE_ID_SEARCH_FIELD, email)
        return if (response.size == 0) {
            null
        } else response.get(0).getId()
    }

    /**
     * Returns the Moodle user badge for the given Moodle user ID.
     *
     *
     * Throws a NOT_FOUND API exception if the Moodle API call returns an error because the given
     * Moodle user ID does not exist.
     */
    @Throws(ApiException::class)
    override fun getUserBadge(userMoodleId: Int): List<BadgeDetails>? {
        if (!enableMoodleCalls()) {
            return null
        }
        val response = moodleApiProvider.get().getMoodleBadge(RESPONSE_FORMAT, token, userMoodleId)
        if (response.getException() != null && response.getException().equals(MOODLE_EXCEPTION)) {
            if (response.getErrorcode().equals(MOODLE_USER_NOT_ALLOWED_ERROR_CODE)) {
                throw ApiException(HttpStatus.NOT_FOUND.value(), response.getMessage())
            } else {
                throw ApiException(response.getMessage())
            }
        }
        return response.getBadges()
    }

    companion object {
        private val RESPONSE_FORMAT = "json"
        private val GET_MOODLE_ID_SEARCH_FIELD = "email"
        private val MOODLE_EXCEPTION = "moodle_exception"
        private val MOODLE_USER_NOT_ALLOWED_ERROR_CODE = "guestsarenotallowed"
    }
}
