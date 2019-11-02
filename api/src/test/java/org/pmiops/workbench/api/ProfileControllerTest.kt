package org.pmiops.workbench.api

import com.google.common.truth.Truth.assertThat
import junit.framework.TestCase.fail
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

import java.sql.Timestamp
import java.time.Instant
import java.util.ArrayList
import java.util.Date
import javax.inject.Provider
import javax.mail.MessagingException
import org.apache.commons.lang3.RandomStringUtils
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.pmiops.workbench.auth.ProfileService
import org.pmiops.workbench.auth.UserAuthentication
import org.pmiops.workbench.auth.UserAuthentication.UserType
import org.pmiops.workbench.billing.FreeTierBillingService
import org.pmiops.workbench.compliance.ComplianceService
import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.config.WorkbenchEnvironment
import org.pmiops.workbench.db.dao.AdminActionHistoryDao
import org.pmiops.workbench.db.dao.UserDao
import org.pmiops.workbench.db.dao.UserDataUseAgreementDao
import org.pmiops.workbench.db.dao.UserService
import org.pmiops.workbench.db.model.User
import org.pmiops.workbench.db.model.UserDataUseAgreement
import org.pmiops.workbench.exceptions.BadRequestException
import org.pmiops.workbench.exceptions.ServerErrorException
import org.pmiops.workbench.firecloud.FireCloudService
import org.pmiops.workbench.firecloud.model.NihStatus
import org.pmiops.workbench.google.CloudStorageService
import org.pmiops.workbench.google.DirectoryService
import org.pmiops.workbench.mail.MailService
import org.pmiops.workbench.model.AccessBypassRequest
import org.pmiops.workbench.model.AccessModule
import org.pmiops.workbench.model.CreateAccountRequest
import org.pmiops.workbench.model.DataAccessLevel
import org.pmiops.workbench.model.EmailVerificationStatus
import org.pmiops.workbench.model.InstitutionalAffiliation
import org.pmiops.workbench.model.InvitationVerificationRequest
import org.pmiops.workbench.model.NihToken
import org.pmiops.workbench.model.Profile
import org.pmiops.workbench.model.ResendWelcomeEmailRequest
import org.pmiops.workbench.model.UpdateContactEmailRequest
import org.pmiops.workbench.notebooks.LeonardoNotebooksClient
import org.pmiops.workbench.test.FakeClock
import org.pmiops.workbench.test.FakeLongRandom
import org.pmiops.workbench.test.Providers
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.annotation.DirtiesContext.ClassMode
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration::class)
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProfileControllerTest {

    @Mock
    private val userProvider: Provider<User>? = null
    @Mock
    private val userAuthenticationProvider: Provider<UserAuthentication>? = null
    @Autowired
    private val userDao: UserDao? = null
    @Autowired
    private val adminActionHistoryDao: AdminActionHistoryDao? = null
    @Autowired
    private val userDataUseAgreementDao: UserDataUseAgreementDao? = null
    @Mock
    private val fireCloudService: FireCloudService? = null
    @Mock
    private val leonardoNotebooksClient: LeonardoNotebooksClient? = null
    @Mock
    private val directoryService: DirectoryService? = null
    @Mock
    private val cloudStorageService: CloudStorageService? = null
    @Mock
    private val freeTierBillingService: FreeTierBillingService? = null
    @Mock
    private val complianceTrainingService: ComplianceService? = null
    @Mock
    private val mailService: MailService? = null
    @Mock
    private var userService: UserService? = null

    private var profileController: ProfileController? = null
    private var cloudProfileController: ProfileController? = null
    private var createAccountRequest: CreateAccountRequest? = null
    private var invitationVerificationRequest: InvitationVerificationRequest? = null
    private var googleUser: com.google.api.services.directory.model.User? = null
    private var clock: FakeClock? = null
    private var user: User? = null

    @Rule
    val exception = ExpectedException.none()

    @Before
    @Throws(MessagingException::class)
    fun setUp() {
        val config = generateConfig()

        val environment = WorkbenchEnvironment(true, "appId")
        val cloudEnvironment = WorkbenchEnvironment(false, "appId")
        createAccountRequest = CreateAccountRequest()
        invitationVerificationRequest = InvitationVerificationRequest()
        val profile = Profile()
        profile.setContactEmail(CONTACT_EMAIL)
        profile.setFamilyName(FAMILY_NAME)
        profile.setGivenName(GIVEN_NAME)
        profile.setUsername(USERNAME)
        profile.setCurrentPosition(CURRENT_POSITION)
        profile.setOrganization(ORGANIZATION)
        profile.setAreaOfResearch(RESEARCH_PURPOSE)
        createAccountRequest!!.setProfile(profile)
        createAccountRequest!!.setInvitationKey(INVITATION_KEY)
        invitationVerificationRequest!!.setInvitationKey(INVITATION_KEY)
        googleUser = com.google.api.services.directory.model.User()
        googleUser!!.primaryEmail = PRIMARY_EMAIL
        googleUser!!.changePasswordAtNextLogin = true
        googleUser!!.password = "testPassword"
        googleUser!!.isEnrolledIn2Sv = true

        clock = FakeClock(NOW)

        doNothing().`when`<MailService>(mailService).sendBetaAccessRequestEmail(Mockito.any())
        userService = UserService(
                userProvider,
                userDao,
                adminActionHistoryDao,
                userDataUseAgreementDao,
                clock,
                FakeLongRandom(NONCE_LONG),
                fireCloudService,
                Providers.of(config),
                complianceTrainingService,
                directoryService)
        val profileService = ProfileService(userDao, freeTierBillingService)
        this.profileController = ProfileController(
                profileService,
                userProvider,
                userAuthenticationProvider,
                userDao,
                clock,
                userService,
                fireCloudService,
                directoryService,
                cloudStorageService,
                leonardoNotebooksClient,
                Providers.of(config),
                environment,
                Providers.of(mailService))
        this.cloudProfileController = ProfileController(
                profileService,
                userProvider,
                userAuthenticationProvider,
                userDao,
                clock,
                userService,
                fireCloudService,
                directoryService,
                cloudStorageService,
                leonardoNotebooksClient,
                Providers.of(config),
                cloudEnvironment,
                Providers.of(mailService))
        `when`<User>(directoryService!!.getUser(PRIMARY_EMAIL)).thenReturn(googleUser)
    }

    @Test(expected = BadRequestException::class)
    @Throws(Exception::class)
    fun testCreateAccount_invitationKeyMismatch() {
        `when`(cloudStorageService!!.readInvitationKey()).thenReturn("BLAH")
        profileController!!.createAccount(createAccountRequest!!)
    }

    @Test(expected = BadRequestException::class)
    @Throws(Exception::class)
    fun testInvitationKeyVerification_invitationKeyMismatch() {
        profileController!!.invitationKeyVerification(invitationVerificationRequest!!)
    }

    @Test
    @Throws(Exception::class)
    fun testCreateAccount_success() {
        createUser()
        val user = userDao!!.findUserByEmail(PRIMARY_EMAIL)
        assertThat(user).isNotNull()
        assertThat(user.dataAccessLevelEnum).isEqualTo(DataAccessLevel.UNREGISTERED)
    }

    @Test
    @Throws(Exception::class)
    fun testCreateAccount_invalidUser() {
        `when`(cloudStorageService!!.readInvitationKey()).thenReturn(INVITATION_KEY)
        val accountRequest = CreateAccountRequest()
        accountRequest.setInvitationKey(INVITATION_KEY)
        createAccountRequest!!.getProfile().setUsername("12")
        accountRequest.setProfile(createAccountRequest!!.getProfile())
        exception.expect(BadRequestException::class.java)
        exception.expectMessage(
                "Username should be at least 3 characters and not more than 64 characters")
        profileController!!.createAccount(accountRequest)
    }

    @Test
    @Throws(Exception::class)
    fun testSubmitDemographicSurvey_success() {
        createUser()
        assertThat(profileController!!.submitDemographicsSurvey().statusCode)
                .isEqualTo(HttpStatus.NOT_IMPLEMENTED)
    }

    @Test
    @Throws(Exception::class)
    fun testSubmitDataUseAgreement_success() {
        createUser()
        assertThat(profileController!!.submitDataUseAgreement(DUA_VERSION, "NIH").statusCode)
                .isEqualTo(HttpStatus.OK)
    }

    @Test
    @Throws(Exception::class)
    fun testMe_success() {
        createUser()

        val profile = profileController!!.me.body
        assertProfile(
                profile,
                PRIMARY_EMAIL,
                CONTACT_EMAIL,
                FAMILY_NAME,
                GIVEN_NAME,
                DataAccessLevel.UNREGISTERED,
                TIMESTAMP, null)
        verify<FireCloudService>(fireCloudService).registerUser(CONTACT_EMAIL, GIVEN_NAME, FAMILY_NAME)
    }

    @Test
    @Throws(Exception::class)
    fun testMe_userBeforeNotLoggedInSuccess() {
        createUser()
        var profile = profileController!!.me.body
        assertProfile(
                profile,
                PRIMARY_EMAIL,
                CONTACT_EMAIL,
                FAMILY_NAME,
                GIVEN_NAME,
                DataAccessLevel.UNREGISTERED,
                TIMESTAMP, null)
        verify<FireCloudService>(fireCloudService).registerUser(CONTACT_EMAIL, GIVEN_NAME, FAMILY_NAME)

        // An additional call to getMe() should have no effect.
        clock!!.increment(1)
        profile = profileController!!.me.body
        assertProfile(
                profile,
                PRIMARY_EMAIL,
                CONTACT_EMAIL,
                FAMILY_NAME,
                GIVEN_NAME,
                DataAccessLevel.UNREGISTERED,
                TIMESTAMP, null)
    }

    @Test
    @Throws(Exception::class)
    fun testMe_institutionalAffiliationsAlphabetical() {
        createUser()

        val profile = profileController!!.me.body
        val affiliations = ArrayList<InstitutionalAffiliation>()
        val first = InstitutionalAffiliation()
        first.setRole("test")
        first.setInstitution("Institution")
        val second = InstitutionalAffiliation()
        second.setRole("zeta")
        second.setInstitution("Zeta")
        affiliations.add(first)
        affiliations.add(second)
        profile.setInstitutionalAffiliations(affiliations)
        profileController!!.updateProfile(profile)

        val result = profileController!!.me.body
        assertThat(result.getInstitutionalAffiliations().size()).isEqualTo(2)
        assertThat(result.getInstitutionalAffiliations().get(0)).isEqualTo(first)
        assertThat(result.getInstitutionalAffiliations().get(1)).isEqualTo(second)
    }

    @Test
    @Throws(Exception::class)
    fun testMe_institutionalAffiliationsNotAlphabetical() {
        createUser()

        val profile = profileController!!.me.body
        val affiliations = ArrayList<InstitutionalAffiliation>()
        val first = InstitutionalAffiliation()
        first.setRole("zeta")
        first.setInstitution("Zeta")
        val second = InstitutionalAffiliation()
        second.setRole("test")
        second.setInstitution("Institution")
        affiliations.add(first)
        affiliations.add(second)
        profile.setInstitutionalAffiliations(affiliations)
        profileController!!.updateProfile(profile)

        val result = profileController!!.me.body
        assertThat(result.getInstitutionalAffiliations().size()).isEqualTo(2)
        assertThat(result.getInstitutionalAffiliations().get(0)).isEqualTo(first)
        assertThat(result.getInstitutionalAffiliations().get(1)).isEqualTo(second)
    }

    @Test
    @Throws(Exception::class)
    fun testMe_removeSingleInstitutionalAffiliation() {
        createUser()

        val profile = profileController!!.me.body
        var affiliations = ArrayList<InstitutionalAffiliation>()
        val first = InstitutionalAffiliation()
        first.setRole("test")
        first.setInstitution("Institution")
        val second = InstitutionalAffiliation()
        second.setRole("zeta")
        second.setInstitution("Zeta")
        affiliations.add(first)
        affiliations.add(second)
        profile.setInstitutionalAffiliations(affiliations)
        profileController!!.updateProfile(profile)
        affiliations = ArrayList<InstitutionalAffiliation>()
        affiliations.add(first)
        profile.setInstitutionalAffiliations(affiliations)
        profileController!!.updateProfile(profile)
        val result = profileController!!.me.body
        assertThat(result.getInstitutionalAffiliations().size()).isEqualTo(1)
        assertThat(result.getInstitutionalAffiliations().get(0)).isEqualTo(first)
    }

    @Test
    @Throws(Exception::class)
    fun testMe_removeAllInstitutionalAffiliations() {
        createUser()

        val profile = profileController!!.me.body
        val affiliations = ArrayList<InstitutionalAffiliation>()
        val first = InstitutionalAffiliation()
        first.setRole("test")
        first.setInstitution("Institution")
        val second = InstitutionalAffiliation()
        second.setRole("zeta")
        second.setInstitution("Zeta")
        affiliations.add(first)
        affiliations.add(second)
        profile.setInstitutionalAffiliations(affiliations)
        profileController!!.updateProfile(profile)
        affiliations.clear()
        profile.setInstitutionalAffiliations(affiliations)
        profileController!!.updateProfile(profile)
        val result = profileController!!.me.body
        assertThat(result.getInstitutionalAffiliations().size()).isEqualTo(0)
    }

    @Test
    @Throws(Exception::class)
    fun updateContactEmail_forbidden() {
        createUser()
        user!!.firstSignInTime = Timestamp(Date().time)
        val originalEmail = user!!.contactEmail

        val response = profileController!!.updateContactEmail(
                UpdateContactEmailRequest()
                        .contactEmail("newContactEmail@whatever.com")
                        .username(user!!.email)
                        .creationNonce(NONCE))
        assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        assertThat(user!!.contactEmail).isEqualTo(originalEmail)
    }

    @Test
    @Throws(Exception::class)
    fun updateContactEmail_badRequest() {
        createUser()
        `when`<User>(directoryService!!.resetUserPassword(anyString())).thenReturn(googleUser)
        user!!.firstSignInTime = null
        val originalEmail = user!!.contactEmail

        val response = profileController!!.updateContactEmail(
                UpdateContactEmailRequest()
                        .contactEmail("bad email address *(SD&(*D&F&*(DS ")
                        .username(user!!.email)
                        .creationNonce(NONCE))
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(user!!.contactEmail).isEqualTo(originalEmail)
    }

    @Test
    @Throws(Exception::class)
    fun updateContactEmail_OK() {
        createUser()
        user!!.firstSignInTime = null
        `when`<User>(directoryService!!.resetUserPassword(anyString())).thenReturn(googleUser)

        val response = profileController!!.updateContactEmail(
                UpdateContactEmailRequest()
                        .contactEmail("newContactEmail@whatever.com")
                        .username(user!!.email)
                        .creationNonce(NONCE))
        assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        assertThat(user!!.contactEmail).isEqualTo("newContactEmail@whatever.com")
    }

    @Test
    @Throws(Exception::class)
    fun updateName_alsoUpdatesDua() {
        createUser()
        val profile = profileController!!.me.body
        profile.setGivenName("OldGivenName")
        profile.setFamilyName("OldFamilyName")
        profileController!!.updateProfile(profile)
        profileController!!.submitDataUseAgreement(DUA_VERSION, "O.O.")
        profile.setGivenName("NewGivenName")
        profile.setFamilyName("NewFamilyName")
        profileController!!.updateProfile(profile)
        val duas = userDataUseAgreementDao!!.findByUserIdOrderByCompletionTimeDesc(profile.getUserId())
        assertThat(duas[0].isUserNameOutOfDate).isTrue()
    }

    @Test(expected = BadRequestException::class)
    @Throws(Exception::class)
    fun updateGivenName_badRequest() {
        createUser()
        val profile = profileController!!.me.body
        val newName = "obladidobladalifegoesonyalalalalalifegoesonobladioblada" + "lifegoesonrahlalalalifegoeson"
        profile.setGivenName(newName)
        profileController!!.updateProfile(profile)
    }

    @Test(expected = BadRequestException::class)
    @Throws(Exception::class)
    fun updateFamilyName_badRequest() {
        createUser()
        val profile = profileController!!.me.body
        val newName = "obladidobladalifegoesonyalalalalalifegoesonobladioblada" + "lifegoesonrahlalalalifegoeson"
        profile.setFamilyName(newName)
        profileController!!.updateProfile(profile)
    }

    @Test(expected = BadRequestException::class)
    @Throws(Exception::class)
    fun updateCurrentPosition_badRequest() {
        createUser()
        val profile = profileController!!.me.body
        profile.setCurrentPosition(RandomStringUtils.random(256))
        profileController!!.updateProfile(profile)
    }

    @Test(expected = BadRequestException::class)
    @Throws(Exception::class)
    fun updateOrganization_badRequest() {
        createUser()
        val profile = profileController!!.me.body
        profile.setOrganization(RandomStringUtils.random(256))
        profileController!!.updateProfile(profile)
    }

    @Test
    @Throws(Exception::class)
    fun resendWelcomeEmail_messagingException() {
        createUser()
        user!!.firstSignInTime = null
        `when`<User>(directoryService!!.resetUserPassword(anyString())).thenReturn(googleUser)
        doThrow(MessagingException("exception"))
                .`when`<MailService>(mailService)
                .sendWelcomeEmail(any(), any(), any<User>())

        val response = profileController!!.resendWelcomeEmail(
                ResendWelcomeEmailRequest().username(user!!.email).creationNonce(NONCE))
        assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        // called twice, once during account creation, once on resend
        verify<MailService>(mailService, times(2)).sendWelcomeEmail(any(), any(), any<User>())
        verify(directoryService, times(1)).resetUserPassword(anyString())
    }

    @Test
    @Throws(Exception::class)
    fun resendWelcomeEmail_OK() {
        createUser()
        `when`<User>(directoryService!!.resetUserPassword(anyString())).thenReturn(googleUser)
        doNothing().`when`<MailService>(mailService).sendWelcomeEmail(any(), any(), any<User>())

        val response = profileController!!.resendWelcomeEmail(
                ResendWelcomeEmailRequest().username(user!!.email).creationNonce(NONCE))
        assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        // called twice, once during account creation, once on resend
        verify<MailService>(mailService, times(2)).sendWelcomeEmail(any(), any(), any<User>())
        verify(directoryService, times(1)).resetUserPassword(anyString())
    }

    @Test
    fun testUpdateNihToken() {
        `when`<Any>(fireCloudService!!.postNihCallback(any<Any>()))
                .thenReturn(NihStatus().linkedNihUsername("test").linkExpireTime(500L))
        try {
            createUser()
            profileController!!.updateNihToken(NihToken().jwt("test"))
        } catch (e: Exception) {
            fail()
        }

    }

    @Test(expected = BadRequestException::class)
    fun testUpdateNihToken_badRequest_1() {
        profileController!!.updateNihToken(null)
    }

    @Test(expected = BadRequestException::class)
    fun testUpdateNihToken_badRequest_2() {
        profileController!!.updateNihToken(NihToken())
    }

    @Test(expected = ServerErrorException::class)
    fun testUpdateNihToken_serverError() {
        doThrow(ServerErrorException()).`when`<FireCloudService>(fireCloudService).postNihCallback(any<Any>())
        profileController!!.updateNihToken(NihToken().jwt("test"))
    }

    @Test
    @Throws(Exception::class)
    fun testSyncEraCommons() {
        val nihStatus = NihStatus()
        val linkedUsername = "linked"
        nihStatus.setLinkedNihUsername(linkedUsername)
        nihStatus.setLinkExpireTime(TIMESTAMP.time)
        `when`<Any>(fireCloudService!!.nihStatus).thenReturn(nihStatus)

        createUser()

        profileController!!.syncEraCommonsStatus()
        assertThat(userDao!!.findUserByEmail(PRIMARY_EMAIL).eraCommonsLinkedNihUsername)
                .isEqualTo(linkedUsername)
        assertThat(userDao.findUserByEmail(PRIMARY_EMAIL).eraCommonsLinkExpireTime).isNotNull()
        assertThat(userDao.findUserByEmail(PRIMARY_EMAIL).eraCommonsCompletionTime).isNotNull()
    }

    @Test
    @Throws(Exception::class)
    fun testBypassAccessModule() {
        val profile = createUser()
        userService = spy(userService!!)
        val environment = WorkbenchEnvironment(true, "appId")
        val config = generateConfig()
        val profileService = ProfileService(userDao, freeTierBillingService)
        this.profileController = ProfileController(
                profileService,
                userProvider,
                userAuthenticationProvider,
                userDao,
                clock,
                userService,
                fireCloudService,
                directoryService,
                cloudStorageService,
                leonardoNotebooksClient,
                Providers.of(config),
                environment,
                Providers.of(mailService))
        profileController!!.bypassAccessRequirement(
                profile.getUserId(),
                AccessBypassRequest().isBypassed(true).moduleName(AccessModule.DATA_USE_AGREEMENT))
        verify<UserService>(userService, times(1)).setDataUseAgreementBypassTime(any(), any())
    }

    @Throws(Exception::class)
    private fun createUser(): Profile {
        `when`(cloudStorageService!!.readInvitationKey()).thenReturn(INVITATION_KEY)
        `when`<User>(directoryService!!.createUser(GIVEN_NAME, FAMILY_NAME, USERNAME, CONTACT_EMAIL))
                .thenReturn(googleUser)
        val result = profileController!!.createAccount(createAccountRequest!!).body
        user = userDao!!.findUserByEmail(PRIMARY_EMAIL)
        user!!.emailVerificationStatusEnum = EmailVerificationStatus.SUBSCRIBED
        userDao.save<User>(user)
        `when`(userProvider!!.get()).thenReturn(user)
        `when`(userAuthenticationProvider!!.get())
                .thenReturn(UserAuthentication(user, null, null, UserType.RESEARCHER))
        return result
    }

    private fun assertProfile(
            profile: Profile,
            primaryEmail: String,
            contactEmail: String,
            familyName: String,
            givenName: String,
            dataAccessLevel: DataAccessLevel,
            firstSignInTime: Timestamp,
            contactEmailFailure: Boolean?) {
        assertThat(profile).isNotNull()
        assertThat(profile.getContactEmail()).isEqualTo(contactEmail)
        assertThat(profile.getFamilyName()).isEqualTo(familyName)
        assertThat(profile.getGivenName()).isEqualTo(givenName)
        assertThat(profile.getDataAccessLevel()).isEqualTo(dataAccessLevel)
        assertThat(profile.getContactEmailFailure()).isEqualTo(contactEmailFailure)
        assertUser(primaryEmail, contactEmail, familyName, givenName, dataAccessLevel, firstSignInTime)
    }

    private fun assertUser(
            primaryEmail: String,
            contactEmail: String,
            familyName: String,
            givenName: String,
            dataAccessLevel: DataAccessLevel,
            firstSignInTime: Timestamp) {
        val user = userDao!!.findUserByEmail(primaryEmail)
        assertThat(user).isNotNull()
        assertThat(user.contactEmail).isEqualTo(contactEmail)
        assertThat(user.familyName).isEqualTo(familyName)
        assertThat(user.givenName).isEqualTo(givenName)
        assertThat(user.dataAccessLevelEnum).isEqualTo(dataAccessLevel)
        assertThat(user.firstSignInTime).isEqualTo(firstSignInTime)
        assertThat(user.dataAccessLevelEnum).isEqualTo(dataAccessLevel)
    }

    private fun generateConfig(): WorkbenchConfig {
        val config = WorkbenchConfig.createEmptyConfig()
        config.billing.projectNamePrefix = BILLING_PROJECT_PREFIX
        config.billing.retryCount = 2
        config.firecloud.registeredDomainName = ""
        config.access.enableComplianceTraining = false
        config.admin.adminIdVerification = "adminIdVerify@dummyMockEmail.com"
        // All access modules are enabled for these tests. So completing any one module should maintain
        // UNREGISTERED status.
        config.access.enableComplianceTraining = true
        config.access.enableBetaAccess = true
        config.access.enableEraCommons = true
        config.access.enableDataUseAgreement = true
        return config
    }

    companion object {

        private val NOW = Instant.now()
        private val TIMESTAMP = Timestamp(NOW.toEpochMilli())
        private val NONCE_LONG: Long = 12345
        private val NONCE = java.lang.Long.toString(NONCE_LONG)
        private val USERNAME = "bob"
        private val GIVEN_NAME = "Bob"
        private val FAMILY_NAME = "Bobberson"
        private val CONTACT_EMAIL = "bob@example.com"
        private val INVITATION_KEY = "secretpassword"
        private val PRIMARY_EMAIL = "bob@researchallofus.org"
        private val BILLING_PROJECT_PREFIX = "all-of-us-free-"
        private val ORGANIZATION = "Test"
        private val CURRENT_POSITION = "Tester"
        private val RESEARCH_PURPOSE = "To test things"
        private val DUA_VERSION = 2
    }
}
