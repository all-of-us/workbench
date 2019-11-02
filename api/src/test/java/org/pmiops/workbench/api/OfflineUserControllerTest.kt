package org.pmiops.workbench.api

import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.argThat
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

import com.google.api.services.cloudresourcemanager.model.Project
import java.io.IOException
import java.util.ArrayList
import java.util.Arrays
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.pmiops.workbench.db.dao.UserService
import org.pmiops.workbench.db.model.User
import org.pmiops.workbench.exceptions.NotFoundException
import org.pmiops.workbench.exceptions.ServerErrorException
import org.pmiops.workbench.google.CloudResourceManagerService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OfflineUserControllerTest {

    @Autowired
    private val cloudResourceManagerService: CloudResourceManagerService? = null
    @Autowired
    private val userService: UserService? = null
    @Autowired
    private val offlineUserController: OfflineUserController? = null

    private var incrementedUserId: Long? = 1L

    private val users: List<User>
        get() = Arrays.asList(
                createUser("a@fake-research-aou.org"),
                createUser("b@fake-research-aou.org"),
                createUser("c@fake-research-aou.org"))

    @TestConfiguration
    @Import(OfflineUserController::class)
    @MockBean(CloudResourceManagerService::class, UserService::class)
    internal class Configuration

    @Before
    fun setUp() {
        `when`(userService!!.allUsers).thenReturn(users)
    }

    private fun createUser(email: String): User {
        val user = User()
        user.email = email
        user.userId = incrementedUserId
        incrementedUserId++
        return user
    }

    @Test
    @Throws(org.pmiops.workbench.moodle.ApiException::class, NotFoundException::class)
    fun testBulkSyncTrainingStatus() {
        // Mock out the service under test to simply return the passed user argument.
        doAnswer { i -> i.getArgument(0) }.`when`<UserService>(userService).syncComplianceTrainingStatus(any())
        offlineUserController!!.bulkSyncComplianceTrainingStatus()
        verify<UserService>(userService, times(3)).syncComplianceTrainingStatus(any())
    }

    @Test(expected = ServerErrorException::class)
    @Throws(org.pmiops.workbench.moodle.ApiException::class, NotFoundException::class)
    fun testBulkSyncTrainingStatusWithSingleUserError() {
        doAnswer { i -> i.getArgument(0) }.`when`<UserService>(userService).syncComplianceTrainingStatus(any())
        doThrow(org.pmiops.workbench.moodle.ApiException("Unknown error"))
                .`when`(userService)
                .syncComplianceTrainingStatus(
                        argThat<User> { user -> user.email == "a@fake-research-aou.org" })
        offlineUserController!!.bulkSyncComplianceTrainingStatus()
        // Even when a single call throws an exception, we call the service for all users.
        verify<UserService>(userService, times(3)).syncComplianceTrainingStatus(any())
    }

    @Test
    @Throws(IOException::class, org.pmiops.workbench.firecloud.ApiException::class)
    fun testBulkSyncEraCommonsStatus() {
        doAnswer { i -> i.getArgument(0) }.`when`<UserService>(userService).syncEraCommonsStatusUsingImpersonation(any())
        offlineUserController!!.bulkSyncEraCommonsStatus()
        verify<UserService>(userService, times(3)).syncEraCommonsStatusUsingImpersonation(any())
    }

    @Test(expected = ServerErrorException::class)
    @Throws(ApiException::class, NotFoundException::class, IOException::class, org.pmiops.workbench.firecloud.ApiException::class)
    fun testBulkSyncEraCommonsStatusWithSingleUserError() {
        doAnswer { i -> i.getArgument(0) }.`when`<UserService>(userService).syncEraCommonsStatusUsingImpersonation(any())
        doThrow(org.pmiops.workbench.firecloud.ApiException("Unknown error"))
                .`when`(userService)
                .syncEraCommonsStatusUsingImpersonation(
                        argThat<User> { user -> user.email == "a@fake-research-aou.org" })
        offlineUserController!!.bulkSyncEraCommonsStatus()
        // Even when a single call throws an exception, we call the service for all users.
        verify<UserService>(userService, times(3)).syncEraCommonsStatusUsingImpersonation(any())
    }

    @Test
    fun testBulkProjectAudit() {
        val projectList = ArrayList<Project>()
        doReturn(projectList).`when`<CloudResourceManagerService>(cloudResourceManagerService).getAllProjectsForUser(any())
        offlineUserController!!.bulkAuditProjectAccess()
        verify<CloudResourceManagerService>(cloudResourceManagerService, times(3)).getAllProjectsForUser(any())
    }
}
