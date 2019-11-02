package org.pmiops.workbench.cdr

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import javax.inject.Provider
import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.db.dao.CdrVersionDao
import org.pmiops.workbench.db.model.CdrVersion
import org.pmiops.workbench.db.model.CommonStorageEnums
import org.pmiops.workbench.db.model.User
import org.pmiops.workbench.exceptions.ForbiddenException
import org.pmiops.workbench.firecloud.FireCloudService
import org.pmiops.workbench.model.DataAccessLevel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CdrVersionService @Autowired
constructor(
        private val userProvider: Provider<User>,
        private val configProvider: Provider<WorkbenchConfig>,
        private val fireCloudService: FireCloudService,
        private val cdrVersionDao: CdrVersionDao) {

    /**
     * Sets the active CDR version, after checking to ensure that the requester is in the appropriate
     * authorization domain. If you have already retrieved a workspace for the requester (and thus
     * implicitly know they are in the authorization domain for its CDR version), you can instead just
     * call [CdrVersionContext.setCdrVersionNoCheckAuthDomain] directly.
     *
     * @param version
     */
    fun setCdrVersion(version: CdrVersion) {
        // TODO: map data access level to authorization domain here (RW-943)
        val authorizationDomain = configProvider.get().firecloud.registeredDomainName
        if (!fireCloudService.isUserMemberOfGroup(userProvider.get().email, authorizationDomain)) {
            throw ForbiddenException(
                    "Requester is not a member of $authorizationDomain, cannot access CDR")
        }
        CdrVersionContext.setCdrVersionNoCheckAuthDomain(version)
    }

    /**
     * Retrieve all the CDR versions visible to users with the specified data access level. When
     * [DataAccessLevel.PROTECTED] is provided, CDR versions for both [ ][DataAccessLevel.REGISTERED] and [DataAccessLevel.PROTECTED] are returned. Note: this
     * relies on [User.dataAccessLevel] accurately reflecting that the user is in the
     * authorization domain that has access to the CDR version BigQuery data sets with the matching
     * [DataAccessLevel] values.
     *
     * @param dataAccessLevel the data access level of the user
     * @return a list of [CdrVersion] in descending timestamp, data access level order.
     */
    fun findAuthorizedCdrVersions(dataAccessLevel: DataAccessLevel): List<CdrVersion> {
        val visibleValues = DATA_ACCESS_LEVEL_TO_VISIBLE_VALUES[dataAccessLevel]
                ?: return ImmutableList.of()
        return cdrVersionDao.findByDataAccessLevelInOrderByCreationTimeDescDataAccessLevelDesc(
                visibleValues)
    }

    companion object {

        private val REGISTERED_ONLY = ImmutableSet.of(CommonStorageEnums.dataAccessLevelToStorage(DataAccessLevel.REGISTERED)!!)

        private val REGISTERED_AND_PROTECTED = ImmutableSet.of(
                CommonStorageEnums.dataAccessLevelToStorage(DataAccessLevel.REGISTERED)!!,
                CommonStorageEnums.dataAccessLevelToStorage(DataAccessLevel.PROTECTED)!!)

        private val DATA_ACCESS_LEVEL_TO_VISIBLE_VALUES = ImmutableMap.builder<DataAccessLevel, ImmutableSet<Short>>()
                .put(DataAccessLevel.REGISTERED, REGISTERED_ONLY)
                .put(DataAccessLevel.PROTECTED, REGISTERED_AND_PROTECTED)
                .build()
    }
}
