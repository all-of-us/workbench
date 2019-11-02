package org.pmiops.workbench.api

import com.google.common.truth.Truth.assertThat
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.`when`

import com.google.common.collect.Lists
import java.sql.Timestamp
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.Comparator
import java.util.Random
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.pmiops.workbench.compliance.ComplianceService
import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.config.WorkbenchConfig.FeatureFlagsConfig
import org.pmiops.workbench.db.dao.AdminActionHistoryDao
import org.pmiops.workbench.db.dao.UserDao
import org.pmiops.workbench.db.dao.UserService
import org.pmiops.workbench.db.model.CommonStorageEnums
import org.pmiops.workbench.db.model.User
import org.pmiops.workbench.exceptions.ForbiddenException
import org.pmiops.workbench.firecloud.FireCloudService
import org.pmiops.workbench.google.DirectoryService
import org.pmiops.workbench.model.DataAccessLevel
import org.pmiops.workbench.model.UserResponse
import org.pmiops.workbench.test.FakeClock
import org.pmiops.workbench.test.FakeLongRandom
import org.pmiops.workbench.utils.PaginationToken
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Scope
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserControllerTest {

    @Autowired
    internal var userController: UserController? = null
    @Autowired
    internal var userDao: UserDao? = null
    @Autowired
    internal var fireCloudService: FireCloudService? = null

    @TestConfiguration
    @Import(UserController::class, UserService::class)
    @MockBean(FireCloudService::class, ComplianceService::class, DirectoryService::class, AdminActionHistoryDao::class)
    internal class Configuration {

        @Bean
        @Scope("prototype")
        fun workbenchConfig(): WorkbenchConfig {
            return config
        }

        @Bean
        fun clock(): Clock {
            return CLOCK
        }

        @Bean
        @Scope("prototype")
        fun user(): User {
            return user
        }

        @Bean
        fun random(): Random {
            return FakeLongRandom(123)
        }
    }

    @Before
    fun setUp() {
        config.firecloud = WorkbenchConfig.FireCloudConfig()
        config.featureFlags = FeatureFlagsConfig()

        saveFamily()
    }

    @Test(expected = ForbiddenException::class)
    fun testUnregistered() {
        `when`(fireCloudService!!.isUserMemberOfGroup(any(), any())).thenReturn(false)
        userController!!.user("Robinson", null, null, null).body
    }

    @Test
    fun testUserSearch() {
        `when`(fireCloudService!!.isUserMemberOfGroup(any(), any())).thenReturn(true)
        val john = userDao!!.findUserByEmail("john@lis.org")

        val response = userController!!.user("John", null, null, null).body
        assertThat(response.getUsers()).hasSize(1)
        assertThat(response.getUsers().get(0).getEmail()).isSameAs(john.email)
    }

    @Test
    fun testUserPartialStringSearch() {
        `when`(fireCloudService!!.isUserMemberOfGroup(any(), any())).thenReturn(true)
        val allUsers = Lists.newArrayList(userDao!!.findAll())

        val response = userController!!.user("obin", null, null, null).body

        // We only want to include users that have active billing projects to avoid users not
        // initialized in FC.
        assertThat(response.getUsers()).hasSize(5)
    }

    @Test
    fun testUserEmptyResponse() {
        `when`(fireCloudService!!.isUserMemberOfGroup(any(), any())).thenReturn(true)
        val response = userController!!.user("", null, null, null).body
        assertThat(response.getUsers()).hasSize(0)
    }

    @Test
    fun testUserNoUsersResponse() {
        `when`(fireCloudService!!.isUserMemberOfGroup(any(), any())).thenReturn(true)
        val response = userController!!.user("Smith", null, null, null).body
        assertThat(response.getUsers()).hasSize(0)
    }

    @Test
    fun testInvalidPageTokenCharacters() {
        val response = userController!!.user("Robinson", "Inv@l!dT0k3n#", null, null)
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.body.getUsers()).hasSize(0)
    }

    @Test
    fun testInvalidPageToken() {
        val response = userController!!.user("Robinson", "eyJvZmZzZXQBhcmFtZF9", null, null)
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.body.getUsers()).hasSize(0)
    }

    @Test
    fun testNegativePageOffset() {
        val response = userController!!.user("Robinson", PaginationToken.of(-1).toBase64(), null, null)
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.body.getUsers()).hasSize(0)
    }

    @Test
    fun testUserPageSize() {
        `when`(fireCloudService!!.isUserMemberOfGroup(any(), any())).thenReturn(true)
        val size = 1
        val robinsons_0 = userController!!.user("Robinson", PaginationToken.of(0).toBase64(), size, null).body
        val robinsons_1 = userController!!.user("Robinson", PaginationToken.of(1).toBase64(), size, null).body
        val robinsons_2 = userController!!.user("Robinson", PaginationToken.of(2).toBase64(), size, null).body
        val robinsons_3 = userController!!.user("Robinson", PaginationToken.of(3).toBase64(), size, null).body
        val robinsons_4 = userController!!.user("Robinson", PaginationToken.of(4).toBase64(), size, null).body

        assertThat(robinsons_0.getUsers()).hasSize(size)
        assertThat(robinsons_0.getNextPageToken()).isEqualTo(PaginationToken.of(1).toBase64())
        assertThat(robinsons_1.getUsers()).hasSize(size)
        assertThat(robinsons_1.getNextPageToken()).isEqualTo(PaginationToken.of(2).toBase64())
        assertThat(robinsons_2.getUsers()).hasSize(size)
        assertThat(robinsons_2.getNextPageToken()).isEqualTo(PaginationToken.of(3).toBase64())
        assertThat(robinsons_3.getUsers()).hasSize(size)
        assertThat(robinsons_3.getNextPageToken()).isEqualTo(PaginationToken.of(4).toBase64())
        assertThat(robinsons_4.getUsers()).hasSize(size)
        assertThat(robinsons_4.getNextPageToken()).isEqualTo("")
    }

    @Test
    fun testUserPagedResponses() {
        `when`(fireCloudService!!.isUserMemberOfGroup(any(), any())).thenReturn(true)
        val robinsons_0_1 = userController!!.user("Robinson", PaginationToken.of(0).toBase64(), 2, null).body
        val robinsons_2_3 = userController!!.user("Robinson", PaginationToken.of(1).toBase64(), 2, null).body
        val robinsons_4 = userController!!.user("Robinson", PaginationToken.of(3).toBase64(), 1, null).body

        // Assert the expected size for each page
        assertThat(robinsons_0_1.getUsers()).hasSize(2)
        assertThat(robinsons_2_3.getUsers()).hasSize(2)
        assertThat(robinsons_4.getUsers()).hasSize(1)

        // Assert uniqueness across pages
        assertThat(robinsons_0_1.getUsers()).containsNoneOf(robinsons_2_3, robinsons_4)
        assertThat(robinsons_2_3.getUsers()).containsNoneOf(robinsons_0_1, robinsons_4)
        assertThat(robinsons_4.getUsers()).containsNoneOf(robinsons_0_1, robinsons_2_3)
    }

    @Test
    fun testUserSort() {
        `when`(fireCloudService!!.isUserMemberOfGroup(any(), any())).thenReturn(true)
        val robinsonsAsc = userController!!.user("Robinson", null, null, "asc").body
        val robinsonsDesc = userController!!.user("Robinson", null, null, "desc").body

        // Assert we have the same elements in both responses
        assertThat(robinsonsAsc.getUsers()).containsAllIn(robinsonsDesc.getUsers())

        // Now reverse one and assert both in the same order
        val descendingReversed = Lists.reverse(robinsonsDesc.getUsers())
        assertThat(robinsonsAsc.getUsers()).containsAllIn(descendingReversed).inOrder()

        // Test that JPA sorting is really what we expected it to be by re-sorting one into a new list
        val newAscending = Lists.newArrayList(robinsonsAsc.getUsers())
        newAscending.sort(Comparator.comparing(Function<org.pmiops.workbench.model.User, Any> { org.pmiops.workbench.model.User.getEmail() }))
        assertThat(robinsonsAsc.getUsers()).containsAllIn(newAscending).inOrder()
    }

    /*
   * Testing helpers
   */

    private fun saveFamily() {
        saveUser("jill@lis.org", "Jill", "Robinson", false)
        saveUser("john@lis.org", "John", "Robinson", true)
        saveUser("judy@lis.org", "Judy", "Robinson", true)
        saveUser("maureen@lis.org", "Mauren", "Robinson", true)
        saveUser("penny@lis.org", "Penny", "Robinson", true)
        saveUser("will@lis.org", "Will", "Robinson", true)
        saveUserNotInFirecloud("bob@lis.org", "Bob", "Robinson", true)
    }

    private fun saveUser(email: String, givenName: String, familyName: String, registered: Boolean) {
        val user = User()
        user.email = email
        user.userId = incrementedUserId
        user.givenName = givenName
        user.familyName = familyName
        user.firstSignInTime = Timestamp(CLOCK.instant()!!.toEpochMilli())
        if (registered) {
            user.dataAccessLevel = CommonStorageEnums.dataAccessLevelToStorage(DataAccessLevel.REGISTERED)
        } else {
            user.dataAccessLevel = CommonStorageEnums.dataAccessLevelToStorage(DataAccessLevel.UNREGISTERED)
        }
        incrementedUserId++
        userDao!!.save(user)
    }

    private fun saveUserNotInFirecloud(
            email: String, givenName: String, familyName: String, registered: Boolean) {
        val user = User()
        user.email = email
        user.userId = incrementedUserId
        user.givenName = givenName
        user.familyName = familyName
        if (registered) {
            user.dataAccessLevel = CommonStorageEnums.dataAccessLevelToStorage(DataAccessLevel.REGISTERED)
        } else {
            user.dataAccessLevel = CommonStorageEnums.dataAccessLevelToStorage(DataAccessLevel.UNREGISTERED)
        }
        incrementedUserId++
        userDao!!.save(user)
    }

    companion object {

        private val CLOCK = FakeClock(Instant.now(), ZoneId.systemDefault())
        private val config = WorkbenchConfig()
        private val user = User()
        private var incrementedUserId: Long = 1
    }
}
