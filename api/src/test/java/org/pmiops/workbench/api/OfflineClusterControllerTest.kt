package org.pmiops.workbench.api

import com.google.common.truth.Truth.assertThat
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

import com.google.common.collect.ImmutableList
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.notebooks.NotebooksConfig
import org.pmiops.workbench.notebooks.api.ClusterApi
import org.pmiops.workbench.notebooks.model.Cluster
import org.pmiops.workbench.notebooks.model.ClusterStatus
import org.pmiops.workbench.test.FakeClock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
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
class OfflineClusterControllerTest {

    @Autowired
    internal var clusterApi: ClusterApi? = null
    @Autowired
    internal var controller: OfflineClusterController? = null
    private var projectIdIndex = 0

    @TestConfiguration
    @Import(OfflineClusterController::class)
    internal class Configuration {
        @MockBean
        @Qualifier(NotebooksConfig.SERVICE_CLUSTER_API)
        private val clusterApi: ClusterApi? = null

        @Bean
        fun clock(): Clock {
            return CLOCK
        }

        @Bean
        fun workbenchConfig(): WorkbenchConfig {
            val config = WorkbenchConfig()
            config.firecloud = WorkbenchConfig.FireCloudConfig()
            config.firecloud.clusterMaxAgeDays = MAX_AGE.toDays().toInt()
            config.firecloud.clusterIdleMaxAgeDays = IDLE_MAX_AGE.toDays().toInt()
            return config
        }
    }

    @Before
    fun setUp() {
        CLOCK.setInstant(NOW)
        projectIdIndex = 0
    }

    private fun clusterWithAge(age: Duration): Cluster {
        return clusterWithAgeAndIdle(age, Duration.ZERO)
    }

    private fun clusterWithAgeAndIdle(age: Duration, idleTime: Duration): Cluster {
        // There should only be one cluster per project, so increment an index for
        // each cluster created per test.
        return Cluster()
                .clusterName("all-of-us")
                .googleProject(String.format("proj-%d", projectIdIndex++))
                .status(ClusterStatus.RUNNING)
                .createdDate(NOW.minus(age).toString())
                .dateAccessed(NOW.minus(idleTime).toString())
    }

    @Throws(Exception::class)
    private fun stubClusters(clusters: List<Cluster>) {
        `when`(clusterApi!!.listClusters(any<T>(), any<T>())).thenReturn(clusters)
        for (c in clusters) {
            `when`(clusterApi!!.getCluster(c.getGoogleProject(), c.getClusterName())).thenReturn(c)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testCheckClustersNoResults() {
        stubClusters(ImmutableList.of<Cluster>())
        assertThat(controller!!.checkClusters().body.getClusterDeletionCount()).isEqualTo(0)

        verify<Any>(clusterApi, never()).deleteCluster(any<T>(), any<T>())
    }

    @Test
    @Throws(Exception::class)
    fun testCheckClustersActiveCluster() {
        stubClusters(ImmutableList.of<Cluster>(clusterWithAge(Duration.ofHours(10))))
        assertThat(controller!!.checkClusters().body.getClusterDeletionCount()).isEqualTo(0)

        verify<Any>(clusterApi, never()).deleteCluster(any<T>(), any<T>())
    }

    @Test
    @Throws(Exception::class)
    fun testCheckClustersActiveTooOld() {
        stubClusters(ImmutableList.of<Cluster>(clusterWithAge(MAX_AGE.plusMinutes(5))))
        assertThat(controller!!.checkClusters().body.getClusterDeletionCount()).isEqualTo(1)

        verify<Any>(clusterApi, times(1)).deleteCluster(any<T>(), any<T>())
    }

    @Test
    @Throws(Exception::class)
    fun testCheckClustersIdleYoung() {
        // Running for under the IDLE_MAX_AGE, idle for 10 hours
        stubClusters(
                ImmutableList.of<Cluster>(
                        clusterWithAgeAndIdle(IDLE_MAX_AGE.minusMinutes(10), Duration.ofHours(10))))
        assertThat(controller!!.checkClusters().body.getClusterDeletionCount()).isEqualTo(0)

        verify<Any>(clusterApi, never()).deleteCluster(any<T>(), any<T>())
    }

    @Test
    @Throws(Exception::class)
    fun testCheckClustersIdleOld() {
        // Running for >IDLE_MAX_AGE, idle for 10 hours
        stubClusters(
                ImmutableList.of<Cluster>(
                        clusterWithAgeAndIdle(IDLE_MAX_AGE.plusMinutes(15), Duration.ofHours(10))))
        assertThat(controller!!.checkClusters().body.getClusterDeletionCount()).isEqualTo(1)

        verify<Any>(clusterApi, times(1)).deleteCluster(any<T>(), any<T>())
    }

    @Test
    @Throws(Exception::class)
    fun testCheckClustersBrieflyIdleOld() {
        // Running for >IDLE_MAX_AGE, idle for only 15 minutes
        stubClusters(
                ImmutableList.of<Cluster>(
                        clusterWithAgeAndIdle(IDLE_MAX_AGE.plusMinutes(15), Duration.ofMinutes(15))))
        assertThat(controller!!.checkClusters().body.getClusterDeletionCount()).isEqualTo(0)

        verify<Any>(clusterApi, never()).deleteCluster(any<T>(), any<T>())
    }

    @Test
    @Throws(Exception::class)
    fun testCheckClustersOtherStatusFiltered() {
        stubClusters(
                ImmutableList.of(clusterWithAge(MAX_AGE.plusDays(10)).status(ClusterStatus.DELETING)))
        assertThat(controller!!.checkClusters().body.getClusterDeletionCount()).isEqualTo(0)

        verify<Any>(clusterApi, never()).deleteCluster(any<T>(), any<T>())
    }

    companion object {
        private val NOW = Instant.parse("1988-12-26T00:00:00Z")
        private val CLOCK = FakeClock(NOW, ZoneId.systemDefault())
        private val MAX_AGE = Duration.ofDays(14)
        private val IDLE_MAX_AGE = Duration.ofDays(7)
    }
}
