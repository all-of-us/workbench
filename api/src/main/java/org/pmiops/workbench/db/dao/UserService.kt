package org.pmiops.workbench.db.dao

import com.google.api.client.http.HttpStatusCodes
import java.io.IOException
import java.sql.Timestamp
import java.time.Clock
import java.time.Instant
import java.util.Random
import java.util.function.Function
import java.util.logging.Logger
import java.util.stream.Collectors
import java.util.stream.Stream
import javax.inject.Provider
import org.hibernate.exception.GenericJDBCException
import org.pmiops.workbench.compliance.ComplianceService
import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.db.model.Address
import org.pmiops.workbench.db.model.AdminActionHistory
import org.pmiops.workbench.db.model.CommonStorageEnums
import org.pmiops.workbench.db.model.DemographicSurvey
import org.pmiops.workbench.db.model.InstitutionalAffiliation
import org.pmiops.workbench.db.model.User
import org.pmiops.workbench.db.model.UserDataUseAgreement
import org.pmiops.workbench.exceptions.BadRequestException
import org.pmiops.workbench.exceptions.ConflictException
import org.pmiops.workbench.exceptions.NotFoundException
import org.pmiops.workbench.firecloud.ApiClient
import org.pmiops.workbench.firecloud.FireCloudService
import org.pmiops.workbench.firecloud.api.NihApi
import org.pmiops.workbench.firecloud.model.NihStatus
import org.pmiops.workbench.google.DirectoryService
import org.pmiops.workbench.model.DataAccessLevel
import org.pmiops.workbench.model.EmailVerificationStatus
import org.pmiops.workbench.moodle.model.BadgeDetails
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.orm.jpa.JpaSystemException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * A higher-level service class containing user manipulation and business logic which can't be
 * represented by automatic query generation in UserDao.
 *
 *
 * A large portion of this class is dedicated to:
 *
 *
 * (1) making it easy to consistently modify a subset of fields in a User entry, with retries (2)
 * ensuring we call a single updateDataAccessLevel method whenever a User entry is saved.
 */
@Service
class UserService @Autowired
constructor(
        private val userProvider: Provider<User>,
        private val userDao: UserDao,
        private val adminActionHistoryDao: AdminActionHistoryDao,
        private val userDataUseAgreementDao: UserDataUseAgreementDao,
        private val clock: Clock,
        private val random: Random,
        private val fireCloudService: FireCloudService,
        private val configProvider: Provider<WorkbenchConfig>,
        private val complianceService: ComplianceService,
        private val directoryService: DirectoryService) {

    private val MAX_RETRIES = 3

    val allUsers: List<User>
        get() = userDao.findUsers()

    /**
     * Updates the currently-authenticated user with a modifier function.
     *
     *
     * Ensures that the data access level for the user reflects the state of other fields on the
     * user; handles conflicts with concurrent updates by retrying.
     */
    private fun updateUserWithRetries(modifyUser: Function<User, User>): User {
        val user = userProvider.get()
        return updateUserWithRetries(modifyUser, user)
    }

    /**
     * Updates a user record with a modifier function.
     *
     *
     * Ensures that the data access level for the user reflects the state of other fields on the
     * user; handles conflicts with concurrent updates by retrying.
     */
    fun updateUserWithRetries(userModifier: Function<User, User>, user: User): User {
        var user = user
        var objectLockingFailureCount = 0
        var statementClosedCount = 0
        while (true) {
            user = userModifier.apply(user)
            updateDataAccessLevel(user)
            try {
                user = userDao.save(user)
                return user
            } catch (e: ObjectOptimisticLockingFailureException) {
                if (objectLockingFailureCount < MAX_RETRIES) {
                    user = userDao.findOne(user.userId)
                    objectLockingFailureCount++
                } else {
                    throw ConflictException(
                            String.format(
                                    "Could not update user %s after %d object locking failures",
                                    user.userId, objectLockingFailureCount))
                }
            } catch (e: JpaSystemException) {
                // We don't know why this happens instead of the object locking failure.
                if ((e.cause as GenericJDBCException)
                                .sqlException
                                .message == "Statement closed.") {
                    if (statementClosedCount < MAX_RETRIES) {
                        user = userDao.findOne(user.userId)
                        statementClosedCount++
                    } else {
                        throw ConflictException(
                                String.format(
                                        "Could not update user %s after %d statement closes",
                                        user.userId, statementClosedCount))
                    }
                } else {
                    throw e
                }
            }

        }
    }

    private fun updateDataAccessLevel(user: User) {
        val dataUseAgreementCompliant = (user.dataUseAgreementCompletionTime != null
                || user.dataUseAgreementBypassTime != null
                || !configProvider.get().access.enableDataUseAgreement)
        val eraCommonsCompliant = (user.eraCommonsBypassTime != null
                || !configProvider.get().access.enableEraCommons
                || user.eraCommonsCompletionTime != null)
        val complianceTrainingCompliant = (user.complianceTrainingCompletionTime != null
                || user.complianceTrainingBypassTime != null
                || !configProvider.get().access.enableComplianceTraining)
        val betaAccessGranted = user.betaAccessBypassTime != null || !configProvider.get().access.enableBetaAccess
        val twoFactorAuthComplete = user.twoFactorAuthCompletionTime != null || user.twoFactorAuthBypassTime != null

        // TODO: can take out other checks once we're entirely moved over to the 'module' columns
        val shouldBeRegistered = (!user.disabled
                && complianceTrainingCompliant
                && eraCommonsCompliant
                && betaAccessGranted
                && twoFactorAuthComplete
                && dataUseAgreementCompliant
                && EmailVerificationStatus.SUBSCRIBED.equals(user.emailVerificationStatusEnum))
        val isInGroup = this.fireCloudService.isUserMemberOfGroup(
                user.email, configProvider.get().firecloud.registeredDomainName)
        if (shouldBeRegistered) {
            if (!isInGroup) {
                this.fireCloudService.addUserToGroup(
                        user.email, configProvider.get().firecloud.registeredDomainName)
                log.info(String.format("Added user %s to registered-tier group.", user.email))
            }
            user.dataAccessLevelEnum = DataAccessLevel.REGISTERED
        } else {
            if (isInGroup) {
                this.fireCloudService.removeUserFromGroup(
                        user.email, configProvider.get().firecloud.registeredDomainName)
                log.info(String.format("Removed user %s from registered-tier group.", user.email))
            }
            user.dataAccessLevelEnum = DataAccessLevel.UNREGISTERED
        }
    }

    private fun isServiceAccount(user: User): Boolean {
        return configProvider.get().auth.serviceAccountApiUsers.contains(user.email)
    }

    fun createServiceAccountUser(email: String): User {
        var user: User? = User()
        user!!.dataAccessLevelEnum = DataAccessLevel.PROTECTED
        user.email = email
        user.disabled = false
        user.emailVerificationStatusEnum = EmailVerificationStatus.UNVERIFIED
        try {
            userDao.save(user)
        } catch (e: DataIntegrityViolationException) {
            user = userDao.findUserByEmail(email)
            if (user == null) {
                throw e
            }
            // If a user already existed (due to multiple requests trying to create a user simultaneously)
            // just return it.
        }

        return user
    }

    @JvmOverloads
    fun createUser(
            givenName: String,
            familyName: String,
            email: String,
            contactEmail: String,
            currentPosition: String,
            organization: String,
            areaOfResearch: String,
            address: Address? = null,
            demographicSurvey: DemographicSurvey? = null,
            institutionalAffiliations: List<InstitutionalAffiliation>? = null): User {
        var user: User? = User()
        user!!.creationNonce = Math.abs(random.nextLong())
        user.dataAccessLevelEnum = DataAccessLevel.UNREGISTERED
        user.email = email
        user.contactEmail = contactEmail
        user.currentPosition = currentPosition
        user.organization = organization
        user.areaOfResearch = areaOfResearch
        user.familyName = familyName
        user.givenName = givenName
        user.disabled = false
        user.aboutYou = null
        user.emailVerificationStatusEnum = EmailVerificationStatus.UNVERIFIED
        user.address = address
        user.demographicSurvey = demographicSurvey
        // For existing user that do not have address
        if (address != null) {
            address.user = user
        }
        if (demographicSurvey != null) demographicSurvey.user = user
        if (institutionalAffiliations != null) {
            val u = user
            institutionalAffiliations.forEach { affiliation ->
                affiliation.user = u
                u.addInstitutionalAffiliation(affiliation)
            }
        }
        try {
            userDao.save(user)
        } catch (e: DataIntegrityViolationException) {
            user = userDao.findUserByEmail(email)
            if (user == null) {
                throw e
            }
            // If a user already existed (due to multiple requests trying to create a user simultaneously)
            // just return it.
        }

        return user
    }

    fun submitDataUseAgreement(
            user: User, dataUseAgreementSignedVersion: Int?, initials: String): User {
        // FIXME: this should not be hardcoded
        if (dataUseAgreementSignedVersion != CURRENT_DATA_USE_AGREEMENT_VERSION) {
            throw BadRequestException("Data Use Agreement Version is not up to date")
        }
        val timestamp = Timestamp(clock.instant().toEpochMilli())
        val dataUseAgreement = UserDataUseAgreement()
        dataUseAgreement.dataUseAgreementSignedVersion = dataUseAgreementSignedVersion
        dataUseAgreement.userId = user.userId
        dataUseAgreement.userFamilyName = user.familyName
        dataUseAgreement.userGivenName = user.givenName
        dataUseAgreement.userInitials = initials
        dataUseAgreement.completionTime = timestamp
        userDataUseAgreementDao.save(dataUseAgreement)
        // TODO: Teardown/reconcile duplicated state between the user profile and DUA.
        user.dataUseAgreementCompletionTime = timestamp
        user.dataUseAgreementSignedVersion = dataUseAgreementSignedVersion
        return userDao.save(user)
    }

    @Transactional
    fun setDataUseAgreementNameOutOfDate(newGivenName: String, newFamilyName: String) {
        val dataUseAgreements = userDataUseAgreementDao.findByUserIdOrderByCompletionTimeDesc(
                userProvider.get().userId)
        dataUseAgreements.forEach { dua -> dua.isUserNameOutOfDate = !dua.userGivenName!!.equals(newGivenName, ignoreCase = true) || !dua.userFamilyName!!.equals(newFamilyName, ignoreCase = true) }
        userDataUseAgreementDao.save(dataUseAgreements)
    }

    fun setDataUseAgreementBypassTime(userId: Long?, bypassTime: Timestamp): User {
        val user = userDao.findUserByUserId(userId!!)
        return updateUserWithRetries(
                { u ->
                    u.dataUseAgreementBypassTime = bypassTime
                    u
                },
                user)
    }

    fun setComplianceTrainingBypassTime(userId: Long?, bypassTime: Timestamp): User {
        val user = userDao.findUserByUserId(userId!!)
        return updateUserWithRetries(
                { u ->
                    u.complianceTrainingBypassTime = bypassTime
                    u
                },
                user)
    }

    fun setBetaAccessBypassTime(userId: Long?, bypassTime: Timestamp): User {
        val user = userDao.findUserByUserId(userId!!)
        return updateUserWithRetries(
                { u ->
                    u.betaAccessBypassTime = bypassTime
                    u
                },
                user)
    }

    fun setEmailVerificationBypassTime(userId: Long?, bypassTime: Timestamp): User {
        val user = userDao.findUserByUserId(userId!!)
        return updateUserWithRetries(
                { u ->
                    u.emailVerificationBypassTime = bypassTime
                    u
                },
                user)
    }

    fun setEraCommonsBypassTime(userId: Long?, bypassTime: Timestamp): User {
        val user = userDao.findUserByUserId(userId!!)
        return updateUserWithRetries(
                { u ->
                    u.eraCommonsBypassTime = bypassTime
                    u
                },
                user)
    }

    fun setIdVerificationBypassTime(userId: Long?, bypassTime: Timestamp): User {
        val user = userDao.findUserByUserId(userId!!)
        return updateUserWithRetries(
                { u ->
                    u.idVerificationBypassTime = bypassTime
                    u
                },
                user)
    }

    fun setTwoFactorAuthBypassTime(userId: Long?, bypassTime: Timestamp): User {
        val user = userDao.findUserByUserId(userId!!)
        return updateUserWithRetries(
                { u ->
                    u.twoFactorAuthBypassTime = bypassTime
                    u
                },
                user)
    }

    fun setClusterRetryCount(clusterRetryCount: Int): User {
        return updateUserWithRetries(
                { user ->
                    user.clusterCreateRetries = clusterRetryCount
                    user
                })
    }

    fun setBillingRetryCount(billingRetryCount: Int): User {
        return updateUserWithRetries(
                { user ->
                    user.billingProjectRetries = billingRetryCount
                    user
                })
    }

    fun setDisabledStatus(userId: Long?, disabled: Boolean): User {
        val user = userDao.findUserByUserId(userId!!)
        return updateUserWithRetries(
                { u ->
                    u.disabled = disabled
                    u
                },
                user)
    }

    fun logAdminUserAction(
            targetUserId: Long, targetAction: String, oldValue: Any, newValue: Any) {
        logAdminAction(targetUserId, null, targetAction, oldValue, newValue)
    }

    fun logAdminWorkspaceAction(
            targetWorkspaceId: Long, targetAction: String, oldValue: Any, newValue: Any) {
        logAdminAction(null, targetWorkspaceId, targetAction, oldValue, newValue)
    }

    private fun logAdminAction(
            targetUserId: Long?,
            targetWorkspaceId: Long?,
            targetAction: String,
            oldValue: Any?,
            newValue: Any?) {
        val adminActionHistory = AdminActionHistory()
        adminActionHistory.targetUserId = targetUserId
        adminActionHistory.targetWorkspaceId = targetWorkspaceId
        adminActionHistory.targetAction = targetAction
        adminActionHistory.oldValue = oldValue?.toString() ?: "null"
        adminActionHistory.newValue = newValue?.toString() ?: "null"
        adminActionHistory.adminUserId = userProvider.get().userId
        adminActionHistory.setTimestamp()
        adminActionHistoryDao.save(adminActionHistory)
    }

    fun getContactEmailTaken(contactEmail: String): Boolean {
        return !userDao.findUserByContactEmail(contactEmail).isEmpty()
    }

    /** Find users matching the user's name or email  */
    fun findUsersBySearchString(term: String, sort: Sort): List<User> {
        val dataAccessLevels = Stream.of<Any>(DataAccessLevel.REGISTERED, DataAccessLevel.PROTECTED)
                .map(Function<Any, R> { CommonStorageEnums.dataAccessLevelToStorage(it) })
                .collect(Collectors.toList<Any>())
        return userDao.findUsersByDataAccessLevelsAndSearchString(dataAccessLevels, term, sort)
    }

    /**
     * Updates the given user's training status from Moodle.
     *
     *
     * We can fetch Moodle data for arbitrary users since we use an API key to access Moodle,
     * rather than user-specific OAuth tokens.
     *
     *
     * Overall flow: 1. Check if user have moodle_id, a. if not retrieve it from MOODLE API and
     * save it in the Database 2. Using the MOODLE_ID get user's Badge update the database with a.
     * training completion time as current time b. training expiration date with as returned from
     * MOODLE. 3. If there are no badges for a user set training completion time and expiration date
     * as null
     */
    @Throws(org.pmiops.workbench.moodle.ApiException::class, NotFoundException::class)
    @JvmOverloads
    fun syncComplianceTrainingStatus(user: User = userProvider.get()): User {
        if (isServiceAccount(user)) {
            // Skip sync for service account user rows.
            return user
        }

        val now = Timestamp(clock.instant().toEpochMilli())
        try {
            var moodleId = user.moodleId
            if (moodleId == null) {
                moodleId = complianceService.getMoodleId(user.email)
                if (moodleId == null) {
                    // User has not yet created/logged into MOODLE
                    return user
                }
                user.moodleId = moodleId
            }

            val badgeResponse = complianceService.getUserBadge(moodleId)
            // The assumption here is that the User will always get 1 badge which will be AoU
            if (badgeResponse != null && badgeResponse.size > 0) {
                val badge = badgeResponse[0]
                val badgeExpiration = if (badge.getDateexpire() == null)
                    null
                else
                    Timestamp(java.lang.Long.parseLong(badge.getDateexpire()))

                if (user.complianceTrainingCompletionTime == null) {
                    // This is the user's first time with a Moodle badge response, so we reset the completion
                    // time.
                    user.complianceTrainingCompletionTime = now
                } else if (badgeExpiration != null && !badgeExpiration.equals(user.complianceTrainingExpirationTime)) {
                    // The badge has a new expiration date, suggesting some sort of course completion change.
                    // Reset the completion time.
                    user.complianceTrainingCompletionTime = now
                }

                user.complianceTrainingExpirationTime = badgeExpiration
            } else {
                // Moodle has returned zero badges for the given user -- we should clear the user's
                // training completion & expiration time.
                user.complianceTrainingCompletionTime = null
                user.complianceTrainingExpirationTime = null
            }

            return updateUserWithRetries(
                    { dbUser ->
                        dbUser.moodleId = user.moodleId
                        dbUser.complianceTrainingExpirationTime = user.complianceTrainingExpirationTime
                        dbUser.complianceTrainingCompletionTime = user.complianceTrainingCompletionTime
                        dbUser
                    },
                    user)

        } catch (e: NumberFormatException) {
            log.severe("Incorrect date expire format from Moodle")
            throw e
        } catch (ex: org.pmiops.workbench.moodle.ApiException) {
            if (ex.getCode() === HttpStatus.NOT_FOUND.value()) {
                log.severe(
                        String.format(
                                "Error while retrieving Badge for user %s: %s ",
                                user.userId, ex.getMessage()))
                throw NotFoundException(ex.getMessage())
            }
            throw ex
        }

    }

    /**
     * Updates the given user's eraCommons-related fields with the NihStatus object returned from FC.
     *
     *
     * This method saves the updated user object to the database and returns it.
     */
    private fun setEraCommonsStatus(targetUser: User, nihStatus: NihStatus?): User {
        val now = Timestamp(clock.instant().toEpochMilli())

        return updateUserWithRetries(
                { user ->
                    if (nihStatus != null) {
                        var eraCommonsCompletionTime = user.eraCommonsCompletionTime
                        val nihLinkExpireTime = Timestamp.from(Instant.ofEpochSecond(nihStatus!!.getLinkExpireTime()))

                        // NihStatus should never come back from firecloud with an empty linked username.
                        // If that is the case, there is an error with FC, because we should get a 404
                        // in that case. Leaving the null checking in for code safety reasons

                        if (nihStatus!!.getLinkedNihUsername() == null) {
                            // If FireCloud says we have no NIH link, always clear the completion time.
                            eraCommonsCompletionTime = null
                        } else if (!nihLinkExpireTime.equals(user.eraCommonsLinkExpireTime)) {
                            // If the link expiration time has changed, we treat this as a "new" completion of the
                            // access requirement.
                            eraCommonsCompletionTime = now
                        } else if (nihStatus!!.getLinkedNihUsername() != null && !nihStatus!!
                                        .getLinkedNihUsername()
                                        .equals(user.eraCommonsLinkedNihUsername)) {
                            // If the linked username has changed, we treat this as a new completion time.
                            eraCommonsCompletionTime = now
                        } else if (eraCommonsCompletionTime == null) {
                            // If the user hasn't yet completed this access requirement, set the time to now.
                            eraCommonsCompletionTime = now
                        }

                        user.eraCommonsLinkedNihUsername = nihStatus!!.getLinkedNihUsername()
                        user.eraCommonsLinkExpireTime = nihLinkExpireTime
                        user.eraCommonsCompletionTime = eraCommonsCompletionTime
                    } else {
                        user.eraCommonsLinkedNihUsername = null
                        user.eraCommonsLinkExpireTime = null
                        user.eraCommonsCompletionTime = null
                    }
                    user
                },
                targetUser)
    }

    /** Syncs the eraCommons access module status for the current user.  */
    fun syncEraCommonsStatus(): User {
        val user = userProvider.get()
        val nihStatus = fireCloudService.nihStatus
        return setEraCommonsStatus(user, nihStatus)
    }

    /**
     * Syncs the eraCommons access module status for an arbitrary user.
     *
     *
     * This uses impersonated credentials and should only be called in the context of a cron job or
     * a request from a user with elevated privileges.
     *
     *
     * Returns the updated User object.
     */
    @Throws(IOException::class, org.pmiops.workbench.firecloud.ApiException::class)
    fun syncEraCommonsStatusUsingImpersonation(user: User): User {
        if (isServiceAccount(user)) {
            // Skip sync for service account user rows.
            return user
        }

        val apiClient = fireCloudService.getApiClientWithImpersonation(user.email)
        val api = NihApi(apiClient)
        try {
            val nihStatus = api.nihStatus()
            return setEraCommonsStatus(user, nihStatus)
        } catch (e: org.pmiops.workbench.firecloud.ApiException) {
            if (e.getCode() === HttpStatusCodes.STATUS_CODE_NOT_FOUND) {
                // We'll catch the NOT_FOUND ApiException here, since we expect many users to have an empty
                // eRA Commons linkage.
                log.info(String.format("NIH Status not found for user %s", user.email))
                return user
            } else {
                throw e
            }
        }

    }

    fun syncTwoFactorAuthStatus() {
        syncTwoFactorAuthStatus(userProvider.get())
    }

    /**  */
    fun syncTwoFactorAuthStatus(targetUser: User): User {
        return if (isServiceAccount(targetUser)) {
            // Skip sync for service account user rows.
            targetUser
        } else updateUserWithRetries(
                { user ->
                    val isEnrolledIn2FA = directoryService.getUser(user.email).isEnrolledIn2Sv!!
                    if (isEnrolledIn2FA) {
                        if (user.twoFactorAuthCompletionTime == null) {
                            user.twoFactorAuthCompletionTime = Timestamp(clock.instant().toEpochMilli())
                        }
                    } else {
                        user.twoFactorAuthCompletionTime = null
                    }
                    user
                },
                targetUser)

    }

    companion object {
        private val CURRENT_DATA_USE_AGREEMENT_VERSION = 2
        private val log = Logger.getLogger(UserService::class.java.name)
    }
}
/** Syncs the current user's training status from Moodle.  */
