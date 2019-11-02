package org.pmiops.workbench.db.dao

import com.google.common.truth.Truth.assertThat
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

import java.sql.Timestamp
import java.time.Instant
import java.util.Arrays
import java.util.Random
import javax.inject.Provider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.pmiops.workbench.compliance.ComplianceService
import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.db.model.User
import org.pmiops.workbench.exceptions.NotFoundException
import org.pmiops.workbench.firecloud.FireCloudService
import org.pmiops.workbench.firecloud.model.NihStatus
import org.pmiops.workbench.google.DirectoryService
import org.pmiops.workbench.moodle.model.BadgeDetails
import org.pmiops.workbench.test.FakeClock
import org.pmiops.workbench.test.Providers
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserServiceTest {

    private val EMAIL_ADDRESS = "abc@fake-research-aou.org"

    private var incrementedUserId: Long? = 1L

    @Autowired
    private val userDao: UserDao? = null
    @Mock
    private val adminActionHistoryDao: AdminActionHistoryDao? = null
    @Autowired
    private val userDataUseAgreementDao: UserDataUseAgreementDao? = null
    @Mock
    private val fireCloudService: FireCloudService? = null
    @Mock
    private val complianceService: ComplianceService? = null
    @Mock
    private val directoryService: DirectoryService? = null

    private var userService: UserService? = null

    private var testUser: User? = null

    @Before
    fun setUp() {
        CLOCK.setInstant(Instant.ofEpochMilli(TIMESTAMP_MSECS))
        val configProvider = Providers.of(WorkbenchConfig.createEmptyConfig())
        testUser = insertUser(EMAIL_ADDRESS)

        userService = UserService(
                Providers.of(testUser),
                userDao,
                adminActionHistoryDao,
                userDataUseAgreementDao,
                CLOCK,
                Random(),
                fireCloudService,
                configProvider,
                complianceService,
                directoryService)
    }

    private fun insertUser(email: String): User {
        val user = User()
        user.email = email
        user.userId = incrementedUserId
        incrementedUserId++
        userDao!!.save(user)
        return user
    }

    @Test
    @Throws(Exception::class)
    fun testSyncComplianceTrainingStatus() {
        val badge = BadgeDetails()
        badge.setName("All of us badge")
        badge.setDateexpire("12345")

        `when`(complianceService!!.getMoodleId(EMAIL_ADDRESS)).thenReturn(1)
        `when`<List<BadgeDetails>>(complianceService.getUserBadge(1)).thenReturn(Arrays.asList<T>(badge))

        userService!!.syncComplianceTrainingStatus()

        // The user should be updated in the database with a non-empty completion and expiration time.
        val user = userDao!!.findUserByEmail(EMAIL_ADDRESS)
        assertThat(user.complianceTrainingCompletionTime)
                .isEqualTo(Timestamp(TIMESTAMP_MSECS))
        assertThat(user.complianceTrainingExpirationTime).isEqualTo(Timestamp(12345))

        // Completion timestamp should not change when the method is called again.
        CLOCK.increment(1000)
        val completionTime = user.complianceTrainingCompletionTime
        userService!!.syncComplianceTrainingStatus()
        assertThat(user.complianceTrainingCompletionTime).isEqualTo(completionTime)
    }

    @Test
    @Throws(Exception::class)
    fun testSyncComplianceTrainingStatusNoMoodleId() {
        `when`(complianceService!!.getMoodleId(EMAIL_ADDRESS)).thenReturn(null)
        userService!!.syncComplianceTrainingStatus()

        verify(complianceService, never()).getUserBadge(anyInt())
        val user = userDao!!.findUserByEmail(EMAIL_ADDRESS)
        assertThat(user.complianceTrainingCompletionTime).isNull()
    }

    @Test
    @Throws(Exception::class)
    fun testSyncComplianceTrainingStatusNullBadge() {
        // When Moodle returns an empty badge response, we should clear the completion bit.
        var user = userDao!!.findUserByEmail(EMAIL_ADDRESS)
        user.complianceTrainingCompletionTime = Timestamp(12345)
        userDao.save(user)

        `when`(complianceService!!.getMoodleId(EMAIL_ADDRESS)).thenReturn(1)
        `when`<List<BadgeDetails>>(complianceService.getUserBadge(1)).thenReturn(null)
        userService!!.syncComplianceTrainingStatus()
        user = userDao.findUserByEmail(EMAIL_ADDRESS)
        assertThat(user.complianceTrainingCompletionTime).isNull()
    }

    @Test(expected = NotFoundException::class)
    @Throws(Exception::class)
    fun testSyncComplianceTrainingStatusBadgeNotFound() {
        // We should propagate a NOT_FOUND exception from the compliance service.
        `when`(complianceService!!.getMoodleId(EMAIL_ADDRESS)).thenReturn(1)
        `when`<List<BadgeDetails>>(complianceService.getUserBadge(1))
                .thenThrow(
                        org.pmiops.workbench.moodle.ApiException(
                                HttpStatus.NOT_FOUND.value(), "user not found"))
        userService!!.syncComplianceTrainingStatus()
    }

    @Test
    @Throws(Exception::class)
    fun testSyncEraCommonsStatus() {
        val nihStatus = NihStatus()
        nihStatus.setLinkedNihUsername("nih-user")
        // FireCloud stores the NIH status in seconds, not msecs.
        nihStatus.setLinkExpireTime(TIMESTAMP_MSECS / 1000)

        `when`<Any>(fireCloudService!!.nihStatus).thenReturn(nihStatus)

        userService!!.syncEraCommonsStatus()

        val user = userDao!!.findUserByEmail(EMAIL_ADDRESS)
        assertThat(user.eraCommonsCompletionTime).isEqualTo(Timestamp(TIMESTAMP_MSECS))
        assertThat(user.eraCommonsLinkExpireTime).isEqualTo(Timestamp(TIMESTAMP_MSECS / 1000))
        assertThat(user.eraCommonsLinkedNihUsername).isEqualTo("nih-user")

        // Completion timestamp should not change when the method is called again.
        CLOCK.increment(1000)
        val completionTime = user.eraCommonsCompletionTime
        userService!!.syncEraCommonsStatus()
        assertThat(user.eraCommonsCompletionTime).isEqualTo(completionTime)
    }

    @Test
    @Throws(Exception::class)
    fun testClearsEraCommonsStatus() {
        // Put the test user in a state where eRA commons is completed.
        testUser!!.eraCommonsCompletionTime = Timestamp(TIMESTAMP_MSECS)
        testUser!!.eraCommonsLinkedNihUsername = "nih-user"
        userDao!!.save<User>(testUser)

        // API returns a null value.
        `when`<Any>(fireCloudService!!.nihStatus).thenReturn(null)

        userService!!.syncEraCommonsStatus()

        val user = userDao.findUserByEmail(EMAIL_ADDRESS)
        assertThat(user.eraCommonsCompletionTime).isNull()
    }

    @Test
    @Throws(Exception::class)
    fun testSyncTwoFactorAuthStatus() {
        val googleUser = com.google.api.services.directory.model.User()
        googleUser.primaryEmail = EMAIL_ADDRESS
        googleUser.isEnrolledIn2Sv = true

        `when`<User>(directoryService!!.getUser(EMAIL_ADDRESS)).thenReturn(googleUser)
        userService!!.syncTwoFactorAuthStatus()
        // twoFactorAuthCompletionTime should now be set
        var user = userDao!!.findUserByEmail(EMAIL_ADDRESS)
        assertThat(user.twoFactorAuthCompletionTime).isNotNull()

        // twoFactorAuthCompletionTime should not change when already set
        CLOCK.increment(1000)
        val twoFactorAuthCompletionTime = user.twoFactorAuthCompletionTime
        userService!!.syncTwoFactorAuthStatus()
        user = userDao.findUserByEmail(EMAIL_ADDRESS)
        assertThat(user.twoFactorAuthCompletionTime).isEqualTo(twoFactorAuthCompletionTime)

        // unset 2FA in google and check that twoFactorAuthCompletionTime is set to null
        googleUser.isEnrolledIn2Sv = false
        userService!!.syncTwoFactorAuthStatus()
        user = userDao.findUserByEmail(EMAIL_ADDRESS)
        assertThat(user.twoFactorAuthCompletionTime).isNull()
    }

    companion object {

        // An arbitrary timestamp to use as the anchor time for access module test cases.
        private val TIMESTAMP_MSECS: Long = 100
        private val CLOCK = FakeClock()
    }
}
