package org.pmiops.workbench.api

import com.google.common.truth.Truth.assertThat
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.`when`

import java.time.Instant
import javax.inject.Provider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.pmiops.workbench.compliance.ComplianceService
import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.db.dao.AdminActionHistoryDao
import org.pmiops.workbench.db.dao.UserDao
import org.pmiops.workbench.db.dao.UserDataUseAgreementDao
import org.pmiops.workbench.db.dao.UserService
import org.pmiops.workbench.db.model.User
import org.pmiops.workbench.firecloud.FireCloudService
import org.pmiops.workbench.firecloud.model.ManagedGroupWithMembers
import org.pmiops.workbench.google.DirectoryService
import org.pmiops.workbench.model.EmptyResponse
import org.pmiops.workbench.model.UpdateUserDisabledRequest
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
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AuthDomainControllerTest {

    @Mock
    private val adminActionHistoryDao: AdminActionHistoryDao? = null
    @Mock
    private val fireCloudService: FireCloudService? = null
    @Mock
    private val userProvider: Provider<User>? = null
    @Mock
    private val complianceService: ComplianceService? = null
    @Mock
    private val directoryService: DirectoryService? = null
    @Autowired
    private val userDao: UserDao? = null
    @Mock
    private val userDataUseAgreementDao: UserDataUseAgreementDao? = null

    private var authDomainController: AuthDomainController? = null

    @Before
    fun setUp() {
        val adminUser = User()
        adminUser.userId = 0L
        doNothing().`when`<FireCloudService>(fireCloudService).addUserToBillingProject(any(), any())
        doNothing().`when`<FireCloudService>(fireCloudService).removeUserFromBillingProject(any(), any())
        `when`<Any>(fireCloudService!!.createGroup(any())).thenReturn(ManagedGroupWithMembers())
        `when`(userProvider!!.get()).thenReturn(adminUser)
        val config = WorkbenchConfig()
        config.firecloud = WorkbenchConfig.FireCloudConfig()
        config.firecloud.registeredDomainName = ""
        config.access = WorkbenchConfig.AccessConfig()
        config.access.enableDataUseAgreement = true
        val clock = FakeClock(Instant.now())
        val userService = UserService(
                userProvider,
                userDao,
                adminActionHistoryDao,
                userDataUseAgreementDao,
                clock,
                FakeLongRandom(12345),
                fireCloudService,
                Providers.of(config),
                complianceService,
                directoryService)
        this.authDomainController = AuthDomainController(fireCloudService, userService, userDao)
    }

    @Test
    fun testCreateAuthDomain() {
        val response = this.authDomainController!!.createAuthDomain("")
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun testDisableUser() {
        createUser(false)
        val request = UpdateUserDisabledRequest().email(PRIMARY_EMAIL).disabled(true)
        val response = this.authDomainController!!.updateUserDisabledStatus(request)
        assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        val updatedUser = userDao!!.findUserByEmail(PRIMARY_EMAIL)
        assertThat(updatedUser.disabled)
    }

    @Test
    fun testEnableUser() {
        createUser(true)
        val request = UpdateUserDisabledRequest().email(PRIMARY_EMAIL).disabled(false)
        val response = this.authDomainController!!.updateUserDisabledStatus(request)
        assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        val updatedUser = userDao!!.findUserByEmail(PRIMARY_EMAIL)
        assertThat(!updatedUser.disabled)
    }

    private fun createUser(disabled: Boolean) {
        val user = User()
        user.givenName = GIVEN_NAME
        user.familyName = FAMILY_NAME
        user.email = PRIMARY_EMAIL
        user.contactEmail = CONTACT_EMAIL
        user.organization = ORGANIZATION
        user.currentPosition = CURRENT_POSITION
        user.areaOfResearch = RESEARCH_PURPOSE
        user.disabled = disabled
        userDao!!.save(user)
    }

    companion object {

        private val GIVEN_NAME = "Bob"
        private val FAMILY_NAME = "Bobberson"
        private val CONTACT_EMAIL = "bob@example.com"
        private val PRIMARY_EMAIL = "bob@researchallofus.org"
        private val ORGANIZATION = "Test"
        private val CURRENT_POSITION = "Tester"
        private val RESEARCH_PURPOSE = "To test things"
    }
}
