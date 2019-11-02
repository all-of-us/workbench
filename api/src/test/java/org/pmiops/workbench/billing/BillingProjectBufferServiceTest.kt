package org.pmiops.workbench.billing

import org.assertj.core.api.Assertions.assertThat
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.pmiops.workbench.db.model.BillingProjectBufferEntry.BillingProjectBufferStatus.ASSIGNED
import org.pmiops.workbench.db.model.BillingProjectBufferEntry.BillingProjectBufferStatus.ASSIGNING
import org.pmiops.workbench.db.model.BillingProjectBufferEntry.BillingProjectBufferStatus.AVAILABLE
import org.pmiops.workbench.db.model.BillingProjectBufferEntry.BillingProjectBufferStatus.CREATING
import org.pmiops.workbench.db.model.BillingProjectBufferEntry.BillingProjectBufferStatus.ERROR

import java.sql.Timestamp
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.ArrayList
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import javax.inject.Provider
import javax.persistence.EntityManager
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.pmiops.workbench.CallsRealMethodsWithDelay
import org.pmiops.workbench.TestLock
import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.db.dao.BillingProjectBufferEntryDao
import org.pmiops.workbench.db.dao.UserDao
import org.pmiops.workbench.db.model.BillingProjectBufferEntry
import org.pmiops.workbench.db.model.StorageEnums
import org.pmiops.workbench.db.model.User
import org.pmiops.workbench.exceptions.NotFoundException
import org.pmiops.workbench.exceptions.WorkbenchException
import org.pmiops.workbench.firecloud.FireCloudService
import org.pmiops.workbench.firecloud.model.BillingProjectStatus
import org.pmiops.workbench.firecloud.model.BillingProjectStatus.CreationStatusEnum
import org.pmiops.workbench.test.FakeClock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Scope
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.annotation.DirtiesContext.MethodMode
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@RunWith(SpringRunner::class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration::class)
class BillingProjectBufferServiceTest {

    @Autowired
    internal var entityManager: EntityManager? = null
    @Autowired
    private val clock: Clock? = null
    @Autowired
    private val userDao: UserDao? = null
    @Autowired
    private val workbenchConfigProvider: Provider<WorkbenchConfig>? = null
    @Autowired
    private var billingProjectBufferEntryDao: BillingProjectBufferEntryDao? = null
    @Autowired
    private val fireCloudService: FireCloudService? = null

    @Autowired
    private var billingProjectBufferService: BillingProjectBufferService? = null

    private val BUFFER_CAPACITY: Long = 5

    private val currentTimestamp: Timestamp
        get() = Timestamp(NOW.toEpochMilli())

    @TestConfiguration
    @Import(BillingProjectBufferService::class)
    @MockBean(FireCloudService::class)
    internal class Configuration {
        @Bean
        fun clock(): Clock {
            return CLOCK
        }

        @Bean
        @Scope("prototype")
        fun workbenchConfig(): WorkbenchConfig? {
            return workbenchConfig
        }
    }

    @Before
    fun setUp() {
        workbenchConfig = WorkbenchConfig.createEmptyConfig()
        workbenchConfig!!.billing.projectNamePrefix = "test-prefix"
        workbenchConfig!!.billing.bufferCapacity = BUFFER_CAPACITY.toInt()
        workbenchConfig!!.billing.bufferRefillProjectsPerTask = 1

        billingProjectBufferEntryDao = spy(billingProjectBufferEntryDao!!)
        val lock = TestLock()
        doAnswer { invocation -> lock.lock() }.`when`<BillingProjectBufferEntryDao>(billingProjectBufferEntryDao).acquireAssigningLock()
        doAnswer { invocation -> lock.release() }
                .`when`<BillingProjectBufferEntryDao>(billingProjectBufferEntryDao)
                .releaseAssigningLock()

        billingProjectBufferService = BillingProjectBufferService(
                billingProjectBufferEntryDao, clock, fireCloudService, workbenchConfigProvider)
    }

    @Test
    fun canBufferMultipleProjectsPerTask() {
        workbenchConfig!!.billing.bufferRefillProjectsPerTask = 2
        billingProjectBufferService!!.bufferBillingProjects()

        verify<FireCloudService>(fireCloudService, times(2)).createAllOfUsBillingProject(anyString())
    }

    @Test
    fun fillBuffer() {
        billingProjectBufferService!!.bufferBillingProjects()

        val captor = ArgumentCaptor.forClass(String::class.java)
        verify<FireCloudService>(fireCloudService).createAllOfUsBillingProject(captor.capture())

        val billingProjectName = captor.value

        assertThat(billingProjectName).startsWith(workbenchConfig!!.billing.projectNamePrefix)
        assertThat<BillingProjectBufferStatus>(
                billingProjectBufferEntryDao!!
                        .findByFireCloudProjectName(billingProjectName)
                        .statusEnum)
                .isEqualTo(CREATING)
    }

    @Test
    fun fillBuffer_failedCreateRequest() {
        val expectedCount = billingProjectBufferEntryDao!!.count() + 1

        doThrow(RuntimeException::class.java).`when`<FireCloudService>(fireCloudService).createAllOfUsBillingProject(anyString())
        try {
            billingProjectBufferService!!.bufferBillingProjects()
        } catch (e: Exception) {
        }

        assertThat(billingProjectBufferEntryDao!!.count()).isEqualTo(expectedCount)
    }

    @Test
    fun fillBuffer_prefixName() {
        workbenchConfig!!.billing.projectNamePrefix = "test-prefix-"
        billingProjectBufferService!!.bufferBillingProjects()

        val captor = ArgumentCaptor.forClass(String::class.java)
        verify<FireCloudService>(fireCloudService).createAllOfUsBillingProject(captor.capture())

        val billingProjectName = captor.value

        assertThat(billingProjectName).doesNotContain("--")
    }

    @Test
    fun fillBuffer_capacity() {
        // fill up buffer
        for (i in 0 until BUFFER_CAPACITY) {
            billingProjectBufferService!!.bufferBillingProjects()
        }
        var expectedCallCount = 0
        billingProjectBufferService!!.bufferBillingProjects()
        verify<FireCloudService>(fireCloudService, times(BUFFER_CAPACITY.toInt() + expectedCallCount))
                .createAllOfUsBillingProject(anyString())

        // free up buffer
        val entry = billingProjectBufferEntryDao!!.findAll().iterator().next()
        entry.setStatusEnum(ASSIGNED, Supplier<Timestamp> { this.getCurrentTimestamp() })
        billingProjectBufferEntryDao!!.save(entry)
        expectedCallCount++
        billingProjectBufferService!!.bufferBillingProjects()
        verify<FireCloudService>(fireCloudService, times(BUFFER_CAPACITY.toInt() + expectedCallCount))
                .createAllOfUsBillingProject(anyString())

        // increase buffer capacity
        expectedCallCount++
        workbenchConfig!!.billing.bufferCapacity = BUFFER_CAPACITY.toInt() + 1
        billingProjectBufferService!!.bufferBillingProjects()
        verify<FireCloudService>(fireCloudService, times(BUFFER_CAPACITY.toInt() + expectedCallCount))
                .createAllOfUsBillingProject(anyString())

        // should be at capacity
        billingProjectBufferService!!.bufferBillingProjects()
        verify<FireCloudService>(fireCloudService, times(BUFFER_CAPACITY.toInt() + expectedCallCount))
                .createAllOfUsBillingProject(anyString())
    }

    @Test
    fun fillBuffer_decreaseCapacity() {
        // fill up buffer
        for (i in 0 until BUFFER_CAPACITY) {
            billingProjectBufferService!!.bufferBillingProjects()
        }

        workbenchConfig!!.billing.bufferCapacity = BUFFER_CAPACITY.toInt() - 2

        // should no op since we're at capacity + 2
        billingProjectBufferService!!.bufferBillingProjects()
        verify<FireCloudService>(fireCloudService, times(BUFFER_CAPACITY.toInt())).createAllOfUsBillingProject(anyString())

        // should no op since we're at capacity + 1
        val bufferEntries = billingProjectBufferEntryDao!!.findAll().iterator()
        var entry = bufferEntries.next()
        entry.setStatusEnum(ASSIGNED, Supplier<Timestamp> { this.getCurrentTimestamp() })
        billingProjectBufferEntryDao!!.save(entry)
        billingProjectBufferService!!.bufferBillingProjects()
        verify<FireCloudService>(fireCloudService, times(BUFFER_CAPACITY.toInt())).createAllOfUsBillingProject(anyString())

        // should no op since we're at capacity
        entry = bufferEntries.next()
        entry.setStatusEnum(ASSIGNED, Supplier<Timestamp> { this.getCurrentTimestamp() })
        billingProjectBufferEntryDao!!.save(entry)
        billingProjectBufferService!!.bufferBillingProjects()
        verify<FireCloudService>(fireCloudService, times(BUFFER_CAPACITY.toInt())).createAllOfUsBillingProject(anyString())

        // should invoke since we're below capacity
        entry = bufferEntries.next()
        entry.setStatusEnum(ASSIGNED, Supplier<Timestamp> { this.getCurrentTimestamp() })
        billingProjectBufferEntryDao!!.save(entry)
        billingProjectBufferService!!.bufferBillingProjects()
        verify<FireCloudService>(fireCloudService, times(BUFFER_CAPACITY.toInt() + 1))
                .createAllOfUsBillingProject(anyString())
    }

    @Test
    fun syncBillingProjectStatus_creating() {
        billingProjectBufferService!!.bufferBillingProjects()

        val captor = ArgumentCaptor.forClass(String::class.java)
        verify<FireCloudService>(fireCloudService).createAllOfUsBillingProject(captor.capture())
        val billingProjectName = captor.value

        val billingProjectStatus = BillingProjectStatus()
        billingProjectStatus.setCreationStatus(CreationStatusEnum.CREATING)
        doReturn(billingProjectStatus)
                .`when`<FireCloudService>(fireCloudService)
                .getBillingProjectStatus(billingProjectName)

        billingProjectBufferService!!.syncBillingProjectStatus()
        assertThat<BillingProjectBufferStatus>(
                billingProjectBufferEntryDao!!
                        .findByFireCloudProjectName(billingProjectName)
                        .statusEnum)
                .isEqualTo(CREATING)

        billingProjectStatus.setCreationStatus(CreationStatusEnum.ADDINGTOPERIMETER)
        doReturn(billingProjectStatus)
                .`when`<FireCloudService>(fireCloudService)
                .getBillingProjectStatus(billingProjectName)

        billingProjectBufferService!!.syncBillingProjectStatus()
        assertThat<BillingProjectBufferStatus>(
                billingProjectBufferEntryDao!!
                        .findByFireCloudProjectName(billingProjectName)
                        .statusEnum)
                .isEqualTo(CREATING)
    }

    @Test
    fun syncBillingProjectStatus_ready() {
        billingProjectBufferService!!.bufferBillingProjects()

        val captor = ArgumentCaptor.forClass(String::class.java)
        verify<FireCloudService>(fireCloudService).createAllOfUsBillingProject(captor.capture())
        val billingProjectName = captor.value

        val billingProjectStatus = BillingProjectStatus()
        billingProjectStatus.setCreationStatus(CreationStatusEnum.READY)
        doReturn(billingProjectStatus)
                .`when`<FireCloudService>(fireCloudService)
                .getBillingProjectStatus(billingProjectName)
        billingProjectBufferService!!.syncBillingProjectStatus()
        assertThat<BillingProjectBufferStatus>(
                billingProjectBufferEntryDao!!
                        .findByFireCloudProjectName(billingProjectName)
                        .statusEnum)
                .isEqualTo(AVAILABLE)
    }

    @Test
    fun syncBillingProjectStatus_error() {
        billingProjectBufferService!!.bufferBillingProjects()

        val captor = ArgumentCaptor.forClass(String::class.java)
        verify<FireCloudService>(fireCloudService).createAllOfUsBillingProject(captor.capture())
        val billingProjectName = captor.value

        val billingProjectStatus = BillingProjectStatus().creationStatus(CreationStatusEnum.ERROR)
        doReturn(billingProjectStatus)
                .`when`<FireCloudService>(fireCloudService)
                .getBillingProjectStatus(billingProjectName)
        billingProjectBufferService!!.syncBillingProjectStatus()
        assertThat<BillingProjectBufferStatus>(
                billingProjectBufferEntryDao!!
                        .findByFireCloudProjectName(billingProjectName)
                        .statusEnum)
                .isEqualTo(ERROR)
    }

    @Test
    fun syncBillingProjectStatus_notFound() {
        billingProjectBufferService!!.bufferBillingProjects()

        val captor = ArgumentCaptor.forClass(String::class.java)
        verify<FireCloudService>(fireCloudService).createAllOfUsBillingProject(captor.capture())
        val billingProjectName = captor.value

        doThrow(NotFoundException::class.java)
                .`when`<FireCloudService>(fireCloudService)
                .getBillingProjectStatus(billingProjectName)
        billingProjectBufferService!!.syncBillingProjectStatus()

        assertThat<BillingProjectBufferStatus>(
                billingProjectBufferEntryDao!!
                        .findByFireCloudProjectName(billingProjectName)
                        .statusEnum)
                .isEqualTo(CREATING)
    }

    @Test
    fun syncBillingProjectStatus_multiple() {
        billingProjectBufferService!!.bufferBillingProjects()
        billingProjectBufferService!!.bufferBillingProjects()
        billingProjectBufferService!!.bufferBillingProjects()

        val captor = ArgumentCaptor.forClass(String::class.java)
        verify<FireCloudService>(fireCloudService, times(3)).createAllOfUsBillingProject(captor.capture())
        val capturedProjectNames = captor.allValues
        doReturn(BillingProjectStatus().creationStatus(CreationStatusEnum.READY))
                .`when`<FireCloudService>(fireCloudService)
                .getBillingProjectStatus(capturedProjectNames[0])
        doReturn(BillingProjectStatus().creationStatus(CreationStatusEnum.READY))
                .`when`<FireCloudService>(fireCloudService)
                .getBillingProjectStatus(capturedProjectNames[1])
        doReturn(BillingProjectStatus().creationStatus(CreationStatusEnum.ERROR))
                .`when`<FireCloudService>(fireCloudService)
                .getBillingProjectStatus(capturedProjectNames[2])

        billingProjectBufferService!!.syncBillingProjectStatus()
        billingProjectBufferService!!.syncBillingProjectStatus()
        billingProjectBufferService!!.syncBillingProjectStatus()
        billingProjectBufferService!!.syncBillingProjectStatus()

        assertThat(
                billingProjectBufferEntryDao!!.countByStatus(
                        StorageEnums.billingProjectBufferStatusToStorage(CREATING)!!))
                .isEqualTo(0)
        assertThat(
                billingProjectBufferEntryDao!!.countByStatus(
                        StorageEnums.billingProjectBufferStatusToStorage(AVAILABLE)!!))
                .isEqualTo(2)
        assertThat(
                billingProjectBufferEntryDao!!.countByStatus(
                        StorageEnums.billingProjectBufferStatusToStorage(ERROR)!!))
                .isEqualTo(1)
    }

    @Test
    fun syncBillingProjectStatus_iteratePastStallingRequests() {
        billingProjectBufferService!!.bufferBillingProjects()
        billingProjectBufferService!!.bufferBillingProjects()
        billingProjectBufferService!!.bufferBillingProjects()

        val captor = ArgumentCaptor.forClass(String::class.java)
        verify<FireCloudService>(fireCloudService, times(3)).createAllOfUsBillingProject(captor.capture())
        val capturedProjectNames = captor.allValues
        doReturn(BillingProjectStatus().creationStatus(CreationStatusEnum.CREATING))
                .`when`<FireCloudService>(fireCloudService)
                .getBillingProjectStatus(capturedProjectNames[0])
        doThrow(WorkbenchException::class.java)
                .`when`<FireCloudService>(fireCloudService)
                .getBillingProjectStatus(capturedProjectNames[1])
        doReturn(BillingProjectStatus().creationStatus(CreationStatusEnum.READY))
                .`when`<FireCloudService>(fireCloudService)
                .getBillingProjectStatus(capturedProjectNames[2])

        billingProjectBufferService!!.syncBillingProjectStatus()
        billingProjectBufferService!!.syncBillingProjectStatus()
        billingProjectBufferService!!.syncBillingProjectStatus()

        assertThat<BillingProjectBufferStatus>(
                billingProjectBufferEntryDao!!
                        .findByFireCloudProjectName(capturedProjectNames[2])
                        .statusEnum)
                .isEqualTo(AVAILABLE)
    }

    @Test
    fun assignBillingProject() {
        val entry = BillingProjectBufferEntry()
        entry.setStatusEnum(AVAILABLE, Supplier<Timestamp> { this.getCurrentTimestamp() })
        entry.fireCloudProjectName = "test-project-name"
        entry.creationTime = currentTimestamp
        billingProjectBufferEntryDao!!.save(entry)

        val user = mock(User::class.java)
        doReturn("fake-email@aou.org").`when`(user).email

        val assignedEntry = billingProjectBufferService!!.assignBillingProject(user)

        val captor = ArgumentCaptor.forClass(String::class.java)
        val secondCaptor = ArgumentCaptor.forClass(String::class.java)
        verify<FireCloudService>(fireCloudService).addUserToBillingProject(captor.capture(), secondCaptor.capture())
        val invokedEmail = captor.value
        val invokedProjectName = secondCaptor.value

        assertThat(invokedEmail).isEqualTo("fake-email@aou.org")
        assertThat(invokedProjectName).isEqualTo("test-project-name")

        assertThat<BillingProjectBufferStatus>(billingProjectBufferEntryDao!!.findOne(assignedEntry.id).statusEnum)
                .isEqualTo(ASSIGNED)
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DirtiesContext(methodMode = MethodMode.AFTER_METHOD)
    @Throws(InterruptedException::class, ExecutionException::class)
    fun assignBillingProject_locking() {
        val firstEntry = BillingProjectBufferEntry()
        firstEntry.setStatusEnum(AVAILABLE, Supplier<Timestamp> { this.getCurrentTimestamp() })
        firstEntry.fireCloudProjectName = "test-project-name-1"
        firstEntry.creationTime = currentTimestamp
        billingProjectBufferEntryDao!!.save(firstEntry)

        val firstUser = User()
        firstUser.email = "fake-email-1@aou.org"
        userDao!!.save(firstUser)

        val secondEntry = BillingProjectBufferEntry()
        secondEntry.setStatusEnum(AVAILABLE, Supplier<Timestamp> { this.getCurrentTimestamp() })
        secondEntry.fireCloudProjectName = "test-project-name-2"
        secondEntry.creationTime = currentTimestamp
        billingProjectBufferEntryDao!!.save(secondEntry)

        val secondUser = User()
        secondUser.email = "fake-email-2@aou.org"
        userDao.save(secondUser)

        doAnswer(CallsRealMethodsWithDelay(500))
                .`when`<BillingProjectBufferEntryDao>(billingProjectBufferEntryDao)
                .save(any(BillingProjectBufferEntry::class.java))

        val t1 = { billingProjectBufferService!!.assignBillingProject(firstUser) }
        val t2 = { billingProjectBufferService!!.assignBillingProject(secondUser) }

        val callableTasks = ArrayList<Callable<BillingProjectBufferEntry>>()
        callableTasks.add(t1)
        callableTasks.add(t2)

        val executor = Executors.newFixedThreadPool(2)
        val futures = executor.invokeAll(callableTasks)

        assertThat(futures[0].get().fireCloudProjectName)
                .isNotEqualTo(futures[1].get().fireCloudProjectName)
    }

    @Test
    fun cleanBillingBuffer_creating() {
        val entry = BillingProjectBufferEntry()
        entry.setStatusEnum(CREATING, Supplier<Timestamp> { this.getCurrentTimestamp() })
        entry.creationTime = currentTimestamp
        billingProjectBufferEntryDao!!.save(entry)

        CLOCK.setInstant(NOW.plus(61, ChronoUnit.MINUTES))
        billingProjectBufferService!!.cleanBillingBuffer()

        assertThat<BillingProjectBufferStatus>(billingProjectBufferEntryDao!!.findOne(entry.id).statusEnum)
                .isEqualTo(ERROR)
    }

    @Test
    fun cleanBillingBuffer_assigning() {
        val entry = BillingProjectBufferEntry()
        entry.setStatusEnum(ASSIGNING, Supplier<Timestamp> { this.getCurrentTimestamp() })
        entry.creationTime = currentTimestamp
        billingProjectBufferEntryDao!!.save(entry)

        CLOCK.setInstant(NOW.plus(11, ChronoUnit.MINUTES))
        billingProjectBufferService!!.cleanBillingBuffer()

        assertThat<BillingProjectBufferStatus>(billingProjectBufferEntryDao!!.findOne(entry.id).statusEnum)
                .isEqualTo(ERROR)
    }

    @Test
    fun cleanBillingBuffer_ignoreValidEntries() {
        val creating = BillingProjectBufferEntry()
        creating.setStatusEnum(CREATING, Supplier<Timestamp> { this.getCurrentTimestamp() })
        creating.creationTime = currentTimestamp
        billingProjectBufferEntryDao!!.save(creating)

        CLOCK.setInstant(NOW.plus(59, ChronoUnit.MINUTES))
        billingProjectBufferService!!.cleanBillingBuffer()

        assertThat<BillingProjectBufferStatus>(billingProjectBufferEntryDao!!.findOne(creating.id).statusEnum)
                .isEqualTo(CREATING)

        val assigning = BillingProjectBufferEntry()
        assigning.setStatusEnum(ASSIGNING, Supplier<Timestamp> { this.getCurrentTimestamp() })
        assigning.creationTime = currentTimestamp
        billingProjectBufferEntryDao!!.save(assigning)

        CLOCK.setInstant(NOW.plus(9, ChronoUnit.MINUTES))
        billingProjectBufferService!!.cleanBillingBuffer()

        assertThat<BillingProjectBufferStatus>(billingProjectBufferEntryDao!!.findOne(assigning.id).statusEnum)
                .isEqualTo(ASSIGNING)
    }

    companion object {

        private val NOW = Instant.now()
        private val CLOCK = FakeClock(NOW, ZoneId.systemDefault())

        private var workbenchConfig: WorkbenchConfig? = null
    }
}
