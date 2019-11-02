package org.pmiops.workbench.api

import com.google.common.truth.Truth.assertThat

import java.sql.Timestamp
import java.util.Arrays
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.pmiops.workbench.cdr.CdrVersionService
import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.db.dao.CdrVersionDao
import org.pmiops.workbench.db.model.CdrVersion
import org.pmiops.workbench.db.model.User
import org.pmiops.workbench.exceptions.ForbiddenException
import org.pmiops.workbench.firecloud.FireCloudService
import org.pmiops.workbench.model.CdrVersionListResponse
import org.pmiops.workbench.model.DataAccessLevel
import org.pmiops.workbench.test.Providers
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.annotation.DirtiesContext.ClassMode
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@RunWith(SpringRunner::class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration::class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CdrVersionsControllerTest {

    @Autowired
    private val cdrVersionDao: CdrVersionDao? = null

    @Autowired
    private val cdrVersionsController: CdrVersionsController? = null

    private var defaultCdrVersion: CdrVersion? = null
    private var protectedCdrVersion: CdrVersion? = null
    private var user: User? = null

    @TestConfiguration
    @Import(CdrVersionService::class, CdrVersionsController::class)
    @MockBean(FireCloudService::class)
    internal class Configuration {
        @Bean
        fun user(): User? {
            // Allows for wiring of the initial Provider<User>; actual mocking of the
            // user is achieved via setUserProvider().
            return null
        }

        @Bean
        fun workbenchConfig(): WorkbenchConfig {
            return WorkbenchConfig()
        }
    }

    @Before
    fun setUp() {
        user = User()
        user!!.dataAccessLevelEnum = DataAccessLevel.REGISTERED
        cdrVersionsController!!.setUserProvider(Providers.of(user))
        defaultCdrVersion = makeCdrVersion(
                1L, /* isDefault */ true, "Test Registered CDR", 123L, DataAccessLevel.REGISTERED)
        protectedCdrVersion = makeCdrVersion(
                2L, /* isDefault */ false, "Test Protected CDR", 456L, DataAccessLevel.PROTECTED)
    }

    @Test
    fun testGetCdrVersionsRegistered() {
        assertResponse(cdrVersionsController!!.cdrVersions.body, defaultCdrVersion)
    }

    @Test
    fun testGetCdrVersionsProtected() {
        user!!.dataAccessLevelEnum = DataAccessLevel.PROTECTED
        assertResponse(
                cdrVersionsController!!.cdrVersions.body, protectedCdrVersion, defaultCdrVersion)
    }

    @Test(expected = ForbiddenException::class)
    fun testGetCdrVersionsUnregistered() {
        user!!.dataAccessLevelEnum = DataAccessLevel.UNREGISTERED
        cdrVersionsController!!.cdrVersions
    }

    private fun assertResponse(response: CdrVersionListResponse, vararg versions: CdrVersion) {
        assertThat(response.getItems())
                .containsExactly(
                        Arrays.stream(versions).map(CdrVersionsController.TO_CLIENT_CDR_VERSION).toArray())
                .inOrder()
        assertThat(response.getDefaultCdrVersionId())
                .isEqualTo(defaultCdrVersion!!.cdrVersionId.toString())
    }

    private fun makeCdrVersion(
            cdrVersionId: Long,
            isDefault: Boolean,
            name: String,
            creationTime: Long,
            dataAccessLevel: DataAccessLevel): CdrVersion {
        val cdrVersion = CdrVersion()
        cdrVersion.isDefault = isDefault
        cdrVersion.bigqueryDataset = "a"
        cdrVersion.bigqueryProject = "b"
        cdrVersion.cdrDbName = "c"
        cdrVersion.cdrVersionId = cdrVersionId
        cdrVersion.creationTime = Timestamp(creationTime)
        cdrVersion.dataAccessLevelEnum = dataAccessLevel
        cdrVersion.name = name
        cdrVersion.numParticipants = 123
        cdrVersion.releaseNumber = 1.toShort()
        cdrVersionDao!!.save(cdrVersion)
        return cdrVersion
    }
}
