package org.pmiops.workbench.api

import com.google.common.base.Strings
import com.google.common.collect.ImmutableMap
import java.sql.Timestamp
import java.time.Clock
import java.util.ArrayList
import java.util.function.Function
import java.util.logging.Level
import java.util.logging.Logger
import java.util.stream.Collectors
import javax.inject.Provider
import javax.mail.MessagingException
import javax.mail.internet.AddressException
import javax.mail.internet.InternetAddress
import org.pmiops.workbench.annotations.AuthorityRequired
import org.pmiops.workbench.auth.ProfileService
import org.pmiops.workbench.auth.UserAuthentication
import org.pmiops.workbench.auth.UserAuthentication.UserType
import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.config.WorkbenchEnvironment
import org.pmiops.workbench.db.dao.UserDao
import org.pmiops.workbench.db.dao.UserService
import org.pmiops.workbench.db.model.Address
import org.pmiops.workbench.db.model.DemographicSurvey
import org.pmiops.workbench.db.model.InstitutionalAffiliation
import org.pmiops.workbench.db.model.User
import org.pmiops.workbench.exceptions.BadRequestException
import org.pmiops.workbench.exceptions.ConflictException
import org.pmiops.workbench.exceptions.EmailException
import org.pmiops.workbench.exceptions.ForbiddenException
import org.pmiops.workbench.exceptions.NotFoundException
import org.pmiops.workbench.exceptions.ServerErrorException
import org.pmiops.workbench.exceptions.UnauthorizedException
import org.pmiops.workbench.exceptions.WorkbenchException
import org.pmiops.workbench.firecloud.FireCloudService
import org.pmiops.workbench.firecloud.model.BillingProjectMembership.CreationStatusEnum
import org.pmiops.workbench.firecloud.model.JWTWrapper
import org.pmiops.workbench.google.CloudStorageService
import org.pmiops.workbench.google.DirectoryService
import org.pmiops.workbench.mail.MailService
import org.pmiops.workbench.model.AccessBypassRequest
import org.pmiops.workbench.model.Address
import org.pmiops.workbench.model.Authority
import org.pmiops.workbench.model.BillingProjectMembership
import org.pmiops.workbench.model.BillingProjectStatus
import org.pmiops.workbench.model.ContactEmailTakenResponse
import org.pmiops.workbench.model.CreateAccountRequest
import org.pmiops.workbench.model.DemographicSurvey
import org.pmiops.workbench.model.Disability
import org.pmiops.workbench.model.EmailVerificationStatus
import org.pmiops.workbench.model.EmptyResponse
import org.pmiops.workbench.model.InstitutionalAffiliation
import org.pmiops.workbench.model.InvitationVerificationRequest
import org.pmiops.workbench.model.NihToken
import org.pmiops.workbench.model.PageVisit
import org.pmiops.workbench.model.Profile
import org.pmiops.workbench.model.ResendWelcomeEmailRequest
import org.pmiops.workbench.model.UpdateContactEmailRequest
import org.pmiops.workbench.model.UserListResponse
import org.pmiops.workbench.model.UsernameTakenResponse
import org.pmiops.workbench.moodle.ApiException
import org.pmiops.workbench.notebooks.LeonardoNotebooksClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.web.bind.annotation.RestController

/**
 * Contains implementations for all Workbench API methods tagged with "profile".
 *
 *
 * The majority of handlers here are lightweight wrappers which delegate to UserService, where
 * many user-focused database and/or API calls are implemented.
 */
@RestController
class ProfileController @Autowired
internal constructor(
        private val profileService: ProfileService,
        private val userProvider: Provider<User>,
        private val userAuthenticationProvider: Provider<UserAuthentication>,
        private val userDao: UserDao,
        private val clock: Clock,
        private val userService: UserService,
        private val fireCloudService: FireCloudService,
        private val directoryService: DirectoryService,
        private val cloudStorageService: CloudStorageService,
        private val leonardoNotebooksClient: LeonardoNotebooksClient,
        private val workbenchConfigProvider: Provider<WorkbenchConfig>,
        private val workbenchEnvironment: WorkbenchEnvironment,
        private val mailServiceProvider: Provider<MailService>) : ProfileApiDelegate {

    val billingProjects: ResponseEntity<List<BillingProjectMembership>>
        get() {
            val memberships = fireCloudService.billingProjectMemberships
            return ResponseEntity.ok<List<BillingProjectMembership>>(
                    memberships.stream()
                            .map(TO_CLIENT_BILLING_PROJECT_MEMBERSHIP)
                            .collect(Collectors.toList()))
        }

    // Record that the user signed in, and create the user's FireCloud user and free tier billing
    // project if they haven't been created already.
    // This means they can start using the NIH billing account in FireCloud (without access to
    // the CDR); we will probably need a job that deactivates accounts after some period of
    // not accepting the terms of use.
    val me: ResponseEntity<Profile>
        get() {

            val user = initializeUserIfNeeded()
            return getProfileResponse(user)
        }

    val allUsers: ResponseEntity<UserListResponse>
        @AuthorityRequired(Authority.ACCESS_CONTROL_ADMIN)
        get() {
            val response = UserListResponse()
            val responseList = ArrayList<Profile>()
            for (user in userDao.findUsers()) {
                responseList.add(profileService.getProfile(user))
            }
            response.setProfileList(responseList)
            return ResponseEntity.ok<UserListResponse>(response)
        }

    private fun createFirecloudUserAndBillingProject(user: User): String {
        // If the user is already registered, their profile will get updated.
        fireCloudService.registerUser(
                user.contactEmail, user.givenName, user.familyName)
        return createFirecloudBillingProject(user)
    }

    private fun validateStringLength(field: String?, fieldName: String, max: Int, min: Int) {
        if (field == null) {
            throw BadRequestException(String.format("%s cannot be left blank!", fieldName))
        }
        if (field.length > max) {
            throw BadRequestException(
                    String.format("%s length exceeds character limit. (%d)", fieldName, max))
        }
        if (field.length < min) {
            if (min == 1) {
                throw BadRequestException(String.format("%s cannot be left blank.", fieldName))
            } else {
                throw BadRequestException(
                        String.format("%s is under character minimum. (%d)", fieldName, min))
            }
        }
    }

    private fun validateProfileFields(profile: Profile) {
        validateStringLength(profile.getGivenName(), "Given Name", 80, 1)
        validateStringLength(profile.getFamilyName(), "Family Name", 80, 1)
        if (!workbenchConfigProvider.get().featureFlags.enableNewAccountCreation) {
            validateStringLength(profile.getCurrentPosition(), "Current Position", 255, 1)
            validateStringLength(profile.getOrganization(), "Organization", 255, 1)
            validateStringLength(profile.getAreaOfResearch(), "Current Research", 3000, 1)
        } else {
            validateStringLength(profile.getAddress().getStreetAddress1(), "Street Address 1", 255, 5)
            validateStringLength(profile.getAddress().getCity(), "City", 3000, 1)
            validateStringLength(profile.getAddress().getState(), "State", 3000, 1)
            validateStringLength(profile.getAddress().getCountry(), "Country", 3000, 2)
        }
    }

    private fun saveUserWithConflictHandling(user: User): User {
        try {
            return userDao.save(user)
        } catch (e: ObjectOptimisticLockingFailureException) {
            log.log(Level.WARNING, "version conflict for user update", e)
            throw ConflictException("Failed due to concurrent modification")
        }

    }

    private fun createFirecloudBillingProject(user: User): String {
        val workbenchConfig = workbenchConfigProvider.get()
        val suffix: Long
        if (workbenchEnvironment.isDevelopment) {
            // For local development, make one billing project per account based on a hash of the account
            // email, and reuse it across database resets. (Assume we won't have any collisions;
            // if we discover that somebody starts using our namespace, change it up.)
            suffix = Math.abs(user.email!!.hashCode()).toLong()
        } else {
            // In other environments, create a suffix based on the user ID from the database. We will
            // add a suffix if that billing project is already taken. (If the database is reset, we
            // should consider switching the prefix.)
            suffix = user.userId
        }
        // GCP billing project names must be <= 30 characters. The per-user hash, an integer,
        // is <= 10 chars.
        val billingProjectNamePrefix = workbenchConfigProvider.get().billing.projectNamePrefix + suffix
        var billingProjectName = billingProjectNamePrefix
        var numAttempts = 0
        while (numAttempts < MAX_BILLING_PROJECT_CREATION_ATTEMPTS) {
            try {
                fireCloudService.createAllOfUsBillingProject(billingProjectName)
                break
            } catch (e: ConflictException) {
                if (workbenchEnvironment.isDevelopment) {
                    // In local development, just re-use existing projects for the account. (We don't
                    // want to create a new billing project every time the database is reset.)
                    log.log(
                            Level.WARNING,
                            String.format(
                                    "Project with name '%s' already exists; using it.", billingProjectName))
                    break
                } else {
                    numAttempts++
                    // In cloud environments, keep trying billing project names until we find one
                    // that hasn't been used before, or we hit MAX_BILLING_PROJECT_CREATION_ATTEMPTS.
                    billingProjectName = "$billingProjectNamePrefix-$numAttempts"
                }
            }

        }
        if (numAttempts.toLong() == MAX_BILLING_PROJECT_CREATION_ATTEMPTS) {
            throw ServerErrorException(
                    String.format(
                            "Encountered %d billing project name " + "collisions; giving up",
                            MAX_BILLING_PROJECT_CREATION_ATTEMPTS))
        }

        try {
            // If the user is already a member of the billing project, this will have no effect.
            fireCloudService.addUserToBillingProject(user.email, billingProjectName)
        } catch (e: ForbiddenException) {
            // AofU is not the owner of the billing project. This should only happen in local
            // environments (and hopefully never, given the prefix we're using.) If it happens,
            // we may need to pick a different prefix.
            log.log(
                    Level.SEVERE,
                    String.format(
                            "Unable to add user to billing project %s; " + "consider changing billing project prefix",
                            billingProjectName),
                    e)
            throw ServerErrorException("Unable to add user to billing project", e)
        }

        return billingProjectName
    }

    private fun initializeUserIfNeeded(): User {
        val userAuthentication = userAuthenticationProvider.get()
        val user = userAuthentication.user
        if (userAuthentication.userType == UserType.SERVICE_ACCOUNT) {
            // Service accounts don't need further initialization.
            return user
        }

        // On first sign-in, create a FC user, billing project, and set the first sign in time.
        if (user.firstSignInTime == null) {
            // If the user is already registered, their profile will get updated.
            fireCloudService.registerUser(
                    user.contactEmail, user.givenName, user.familyName)

            user.firstSignInTime = Timestamp(clock.instant().toEpochMilli())
            // If the user is logged in, then we know that they have followed the account creation
            // instructions sent to
            // their initial contact email address.
            user.emailVerificationStatusEnum = EmailVerificationStatus.SUBSCRIBED
            return saveUserWithConflictHandling(user)
        }

        return user
    }

    private fun getProfileResponse(user: User): ResponseEntity<Profile> {
        val profile = profileService.getProfile(user)
        // Note: The following requires that the current request is authenticated.
        return ResponseEntity.ok<Profile>(profile)
    }

    fun isUsernameTaken(username: String): ResponseEntity<UsernameTakenResponse> {
        return ResponseEntity.ok(
                UsernameTakenResponse().isTaken(directoryService.isUsernameTaken(username)))
    }

    fun isContactEmailTaken(contactEmail: String): ResponseEntity<ContactEmailTakenResponse> {
        return ResponseEntity.ok(
                ContactEmailTakenResponse().isTaken(userService.getContactEmailTaken(contactEmail)))
    }

    fun createAccount(request: CreateAccountRequest): ResponseEntity<Profile> {
        verifyInvitationKey(request.getInvitationKey())
        val userName = request.getProfile().getUsername()
        if (userName == null || userName!!.length < 3 || userName!!.length > 64)
            throw BadRequestException(
                    "Username should be at least 3 characters and not more than 64 characters")
        request.getProfile().setUsername(request.getProfile().getUsername().toLowerCase())
        validateProfileFields(request.getProfile())
        // This check will be removed once enableNewAccountCreation flag is turned on.
        if (request.getProfile().getAddress() == null) {
            request.getProfile().setAddress(Address())
        }
        if (request.getProfile().getDemographicSurvey() == null) {
            request.getProfile().setDemographicSurvey(DemographicSurvey())
        }
        if (request.getProfile().getInstitutionalAffiliations() == null) {
            request.getProfile().setInstitutionalAffiliations(ArrayList<InstitutionalAffiliation>())
        }
        val googleUser = directoryService.createUser(
                request.getProfile().getGivenName(),
                request.getProfile().getFamilyName(),
                request.getProfile().getUsername(),
                request.getProfile().getContactEmail())

        // Create a user that has no data access or FC user associated.
        // We create this account before they sign in so we can keep track of which users we have
        // created Google accounts for. This can be used subsequently to delete orphaned accounts.

        // We store this information in our own database so that:
        // 1) we can support bring-your-own account in future (when we won't be using directory service)
        // 2) we can easily generate lists of researchers for the storefront, without joining to Google

        // It's possible for the profile information to become out of sync with the user's Google
        // profile, since it can be edited in our UI as well as the Google UI,  and we're fine with
        // that; the expectation is their profile in AofU will be managed in AofU, not in Google.

        val user = userService.createUser(
                request.getProfile().getGivenName(),
                request.getProfile().getFamilyName(),
                googleUser.primaryEmail,
                request.getProfile().getContactEmail(),
                request.getProfile().getCurrentPosition(),
                request.getProfile().getOrganization(),
                request.getProfile().getAreaOfResearch(),
                FROM_CLIENT_ADDRESS.apply(request.getProfile().getAddress()),
                FROM_CLIENT_DEMOGRAPHIC_SURVEY.apply(request.getProfile().getDemographicSurvey()),
                request.getProfile().getInstitutionalAffiliations().stream()
                        .map(FROM_CLIENT_INSTITUTIONAL_AFFILIATION)
                        .collect(Collectors.toList<T>()))

        try {
            mailServiceProvider
                    .get()
                    .sendWelcomeEmail(
                            request.getProfile().getContactEmail(), googleUser.password, googleUser)
        } catch (e: MessagingException) {
            throw WorkbenchException(e)
        }

        // Note: Avoid getProfileResponse() here as this is not an authenticated request.
        return ResponseEntity.ok<Profile>(profileService.getProfile(user))
    }

    fun requestBetaAccess(): ResponseEntity<Profile> {
        val now = Timestamp(clock.instant().toEpochMilli())
        var user = userProvider.get()
        if (user.betaAccessRequestTime == null) {
            log.log(Level.INFO, "Sending beta access request email.")
            try {
                mailServiceProvider.get().sendBetaAccessRequestEmail(user.email)
            } catch (e: MessagingException) {
                throw EmailException("Error submitting beta access request", e)
            }

            user.betaAccessRequestTime = now
            user = saveUserWithConflictHandling(user)
        }
        return getProfileResponse(user)
    }

    fun submitDemographicsSurvey(): ResponseEntity<Profile> {
        // TODO: RW-2517.
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build<Profile>()
    }

    fun submitDataUseAgreement(
            dataUseAgreementSignedVersion: Int?, initials: String): ResponseEntity<Profile> {
        val user = userService.submitDataUseAgreement(
                userProvider.get(), dataUseAgreementSignedVersion, initials)
        return getProfileResponse(saveUserWithConflictHandling(user))
    }

    /**
     * This methods updates logged in user's training status from Moodle.
     *
     * @return Profile updated with training completion time
     */
    fun syncComplianceTrainingStatus(): ResponseEntity<Profile> {
        try {
            userService.syncComplianceTrainingStatus()
        } catch (ex: NotFoundException) {
            throw ex
        } catch (e: ApiException) {
            throw ServerErrorException(e)
        }

        return getProfileResponse(userProvider.get())
    }

    fun syncEraCommonsStatus(): ResponseEntity<Profile> {
        userService.syncEraCommonsStatus()
        return getProfileResponse(userProvider.get())
    }

    fun syncTwoFactorAuthStatus(): ResponseEntity<Profile> {
        userService.syncTwoFactorAuthStatus()
        return getProfileResponse(userProvider.get())
    }

    fun invitationKeyVerification(
            invitationVerificationRequest: InvitationVerificationRequest): ResponseEntity<Void> {
        verifyInvitationKey(invitationVerificationRequest.getInvitationKey())
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }

    private fun verifyInvitationKey(invitationKey: String?) {
        if (invitationKey == null
                || invitationKey == ""
                || invitationKey != cloudStorageService.readInvitationKey()) {
            throw BadRequestException(
                    "Missing or incorrect invitationKey (this API is not yet publicly launched)")
        }
    }

    private fun checkUserCreationNonce(user: User, nonce: String) {
        if (Strings.isNullOrEmpty(nonce)) {
            throw BadRequestException("missing required creationNonce")
        }
        if (user.creationNonce == null || nonce != user.creationNonce!!.toString()) {
            throw UnauthorizedException("invalid creationNonce provided")
        }
    }

    /*
   * This un-authed API method is limited such that we only allow contact email updates before the user has signed in
   * with the newly created gsuite account. Once the user has logged in, they can change their contact email through
   * the normal profile update process.
   */
    fun updateContactEmail(
            updateContactEmailRequest: UpdateContactEmailRequest): ResponseEntity<Void> {
        val username = updateContactEmailRequest.getUsername().toLowerCase()
        val googleUser = directoryService.getUser(username)
        val user = userDao.findUserByEmail(username)
        checkUserCreationNonce(user, updateContactEmailRequest.getCreationNonce())
        if (!userNeverLoggedIn(googleUser, user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
        val newEmail = updateContactEmailRequest.getContactEmail()
        try {
            InternetAddress(newEmail).validate()
        } catch (e: AddressException) {
            log.log(Level.INFO, "Invalid email entered.")
            return ResponseEntity.badRequest().build()
        }

        user.contactEmail = newEmail
        return resetPasswordAndSendWelcomeEmail(username, user)
    }

    fun resendWelcomeEmail(resendRequest: ResendWelcomeEmailRequest): ResponseEntity<Void> {
        val username = resendRequest.getUsername().toLowerCase()
        val googleUser = directoryService.getUser(username)
        val user = userDao.findUserByEmail(username)
        checkUserCreationNonce(user, resendRequest.getCreationNonce())
        return if (!userNeverLoggedIn(googleUser, user)) {
            ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        } else resetPasswordAndSendWelcomeEmail(username, user)
    }

    private fun userNeverLoggedIn(
            googleUser: com.google.api.services.directory.model.User, user: User): Boolean {
        return user.firstSignInTime == null && googleUser.changePasswordAtNextLogin!!
    }

    private fun resetPasswordAndSendWelcomeEmail(username: String, user: User): ResponseEntity<Void> {
        val googleUser = directoryService.resetUserPassword(username)
        try {
            mailServiceProvider
                    .get()
                    .sendWelcomeEmail(user.contactEmail, googleUser.password, googleUser)
        } catch (e: MessagingException) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }

    fun updatePageVisits(newPageVisit: PageVisit): ResponseEntity<Profile> {
        var user = userProvider.get()
        user = userDao.findUserWithAuthoritiesAndPageVisits(user.userId)
        val timestamp = Timestamp(clock.instant().toEpochMilli())
        val shouldAdd = user.pageVisits.stream().noneMatch { v -> v.pageId == newPageVisit.getPage() }
        if (shouldAdd) {
            val firstPageVisit = org.pmiops.workbench.db.model.PageVisit()
            firstPageVisit.pageId = newPageVisit.getPage()
            firstPageVisit.user = user
            firstPageVisit.firstVisit = timestamp
            user.pageVisits.add(firstPageVisit)
            userDao.save(user)
        }
        return getProfileResponse(saveUserWithConflictHandling(user))
    }

    fun updateProfile(updatedProfile: Profile): ResponseEntity<Void> {
        validateProfileFields(updatedProfile)
        val user = userProvider.get()

        if (!userProvider.get().givenName!!.equals(updatedProfile.getGivenName(), ignoreCase = true) || !userProvider.get().familyName!!.equals(updatedProfile.getFamilyName(), ignoreCase = true)) {
            userService.setDataUseAgreementNameOutOfDate(
                    updatedProfile.getGivenName(), updatedProfile.getFamilyName())
        }

        user.givenName = updatedProfile.getGivenName()
        user.familyName = updatedProfile.getFamilyName()
        user.organization = updatedProfile.getOrganization()
        user.currentPosition = updatedProfile.getCurrentPosition()
        user.aboutYou = updatedProfile.getAboutYou()
        user.areaOfResearch = updatedProfile.getAreaOfResearch()

        if (updatedProfile.getContactEmail() != null && !updatedProfile.getContactEmail().equals(user.contactEmail)) {
            // See RW-1488.
            throw BadRequestException("Changing email is not currently supported")
        }
        val newAffiliations = updatedProfile.getInstitutionalAffiliations().stream()
                .map(FROM_CLIENT_INSTITUTIONAL_AFFILIATION)
                .collect(Collectors.toList<T>())
        var i = 0
        val oldAffilations = user.institutionalAffiliations.listIterator()
        var shouldAdd = false
        if (newAffiliations.size == 0) {
            shouldAdd = true
        }
        for (affiliation in newAffiliations) {
            affiliation.orderIndex = i
            affiliation.user = user
            if (oldAffilations.hasNext()) {
                val oldAffilation = oldAffilations.next()
                if (oldAffilation.role != affiliation.role || oldAffilation.institution != affiliation.institution) {
                    shouldAdd = true
                }
            } else {
                shouldAdd = true
            }
            i++
        }
        if (oldAffilations.hasNext()) {
            shouldAdd = true
        }
        if (shouldAdd) {
            user.clearInstitutionalAffiliations()
            for (affiliation in newAffiliations) {
                user.addInstitutionalAffiliation(affiliation)
            }
        }

        // This does not update the name in Google.
        saveUserWithConflictHandling(user)
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }

    @AuthorityRequired(Authority.ACCESS_CONTROL_ADMIN)
    fun getUser(userId: Long?): ResponseEntity<Profile> {
        val user = userDao.findUserByUserId(userId!!)
        return ResponseEntity.ok<Profile>(profileService.getProfile(user))
    }

    @AuthorityRequired(Authority.ACCESS_CONTROL_ADMIN)
    fun bypassAccessRequirement(
            userId: Long?, request: AccessBypassRequest): ResponseEntity<EmptyResponse> {
        updateBypass(userId!!, request)
        return ResponseEntity.ok<EmptyResponse>(EmptyResponse())
    }

    fun unsafeSelfBypassAccessRequirement(
            request: AccessBypassRequest): ResponseEntity<EmptyResponse> {
        if (!workbenchConfigProvider.get().access.unsafeAllowSelfBypass) {
            throw ForbiddenException("Self bypass is disallowed in this environment.")
        }
        val userId = userProvider.get().userId
        updateBypass(userId, request)
        return ResponseEntity.ok<EmptyResponse>(EmptyResponse())
    }

    fun updateNihToken(token: NihToken?): ResponseEntity<Profile> {
        if (token == null || token!!.getJwt() == null) {
            throw BadRequestException("Token is required.")
        }
        val wrapper = JWTWrapper().jwt(token!!.getJwt())
        try {
            fireCloudService.postNihCallback(wrapper)
            userService.syncEraCommonsStatus()
            return getProfileResponse(userProvider.get())
        } catch (e: WorkbenchException) {
            throw e
        }

    }

    private fun updateBypass(userId: Long, request: AccessBypassRequest) {
        val valueToSet: Timestamp?
        val previousValue: Timestamp?
        val bypassed = request.getIsBypassed()
        val user = userDao.findUserByUserId(userId)
        if (bypassed!!) {
            valueToSet = Timestamp(clock.instant().toEpochMilli())
        } else {
            valueToSet = null
        }
        when (request.getModuleName()) {
            DATA_USE_AGREEMENT -> {
                previousValue = user.dataUseAgreementBypassTime
                userService.setDataUseAgreementBypassTime(userId, valueToSet)
            }
            COMPLIANCE_TRAINING -> {
                previousValue = user.complianceTrainingBypassTime
                userService.setComplianceTrainingBypassTime(userId, valueToSet)
            }
            BETA_ACCESS -> {
                previousValue = user.betaAccessBypassTime
                userService.setBetaAccessBypassTime(userId, valueToSet)
            }
            ERA_COMMONS -> {
                previousValue = user.eraCommonsBypassTime
                userService.setEraCommonsBypassTime(userId, valueToSet)
            }
            TWO_FACTOR_AUTH -> {
                previousValue = user.twoFactorAuthBypassTime
                userService.setTwoFactorAuthBypassTime(userId, valueToSet)
            }
            else -> throw BadRequestException(
                    "There is no access module named: " + request.getModuleName().toString())
        }
        userService.logAdminUserAction(
                userId,
                "set bypass status for module " + request.getModuleName().toString() + " to " + bypassed,
                previousValue,
                valueToSet)
    }

    fun deleteProfile(): ResponseEntity<Void> {
        if (!workbenchConfigProvider.get().featureFlags.unsafeAllowDeleteUser) {
            throw ForbiddenException("Self account deletion is disallowed in this environment.")
        }
        val user = userProvider.get()
        log.log(Level.WARNING, "Deleting profile: user email: " + user.email!!)
        directoryService.deleteUser(user.email!!.split("@".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0])
        userDao.delete(user.userId)

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }

    companion object {
        private val fcToWorkbenchBillingMap = ImmutableMap.Builder<CreationStatusEnum, BillingProjectStatus>()
                .put(CreationStatusEnum.CREATING, BillingProjectStatus.PENDING)
                .put(CreationStatusEnum.READY, BillingProjectStatus.READY)
                .put(CreationStatusEnum.ERROR, BillingProjectStatus.ERROR)
                .build()
        private val TO_CLIENT_BILLING_PROJECT_MEMBERSHIP = Function<Any, Any> { billingProjectMembership ->
            val result = BillingProjectMembership()
            result.setProjectName(billingProjectMembership.getProjectName())
            result.setRole(billingProjectMembership.getRole())
            result.setStatus(
                    fcToWorkbenchBillingMap[billingProjectMembership.getCreationStatus()])
            result
        }
        private val FROM_CLIENT_INSTITUTIONAL_AFFILIATION = Function<Any, InstitutionalAffiliation> { institutionalAffiliation ->
            val result = org.pmiops.workbench.db.model.InstitutionalAffiliation()
            if (institutionalAffiliation.getInstitution() != null) {
                result.institution = institutionalAffiliation.getInstitution()
            }
            if (institutionalAffiliation.getNonAcademicAffiliation() != null) {
                result.setNonAcademicAffiliationnEnum(
                        institutionalAffiliation.getNonAcademicAffiliation())
            }

            result.role = institutionalAffiliation.getRole()
            result.other = institutionalAffiliation.getOther()

            result
        }

        private val FROM_CLIENT_ADDRESS = Function<Any, Address> { address ->
            val result = org.pmiops.workbench.db.model.Address()
            result.streetAddress1 = address.getStreetAddress1()
            result.streetAddress2 = address.getStreetAddress2()
            result.city = address.getCity()
            result.state = address.getState()
            result.zipCode = address.getZipCode()
            result.country = address.getCountry()
            result
        }

        private val FROM_CLIENT_DEMOGRAPHIC_SURVEY = Function<Any, DemographicSurvey> { demographicSurvey ->
            val result = org.pmiops.workbench.db.model.DemographicSurvey()
            if (demographicSurvey.getRace() != null)
                result.raceEnum = demographicSurvey.getRace()
            if (demographicSurvey.getEthnicity() != null)
                result.ethnicityEnum = demographicSurvey.getEthnicity()
            if (demographicSurvey.getDisability() != null)
                result.disabilityEnum = if (demographicSurvey.getDisability()) Disability.TRUE else Disability.FALSE
            if (demographicSurvey.getEducation() != null)
                result.educationEnum = demographicSurvey.getEducation()
            if (demographicSurvey.getGender() != null)
                result.genderEnum = demographicSurvey.getGender()
            if (demographicSurvey.getDisability() != null)
                result.disabilityEnum = if (demographicSurvey.getDisability()) Disability.TRUE else Disability.FALSE
            if (demographicSurvey.getYearOfBirth() != null)
                result.year_of_birth = demographicSurvey.getYearOfBirth().intValue()
            result
        }

        private val log = Logger.getLogger(ProfileController::class.java.name)

        private val MAX_BILLING_PROJECT_CREATION_ATTEMPTS: Long = 5
    }
}
