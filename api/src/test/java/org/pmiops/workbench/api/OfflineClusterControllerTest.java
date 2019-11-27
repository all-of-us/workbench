package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.notebooks.NotebooksConfig;
import org.pmiops.workbench.notebooks.api.ClusterApi;
import org.pmiops.workbench.notebooks.model.Cluster;
import org.pmiops.workbench.notebooks.model.ClusterStatus;
import org.pmiops.workbench.notebooks.model.ListClusterResponse;
import org.pmiops.workbench.test.FakeClock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class OfflineClusterControllerTest {
  private static final Instant NOW = Instant.parse("1988-12-26T00:00:00Z");
  private static final FakeClock CLOCK = new FakeClock(NOW, ZoneId.systemDefault());
  private static final Duration MAX_AGE = Duration.ofDays(14);
  private static final Duration IDLE_MAX_AGE = Duration.ofDays(7);

  @TestConfiguration
  @Import({OfflineClusterController.class})
  static class Configuration {
    @MockBean
    @Qualifier(NotebooksConfig.SERVICE_CLUSTER_API)
    private ClusterApi clusterApi;

    @Bean
    Clock clock() {
      return CLOCK;
    }

    @Bean
    public WorkbenchConfig workbenchConfig() {
      WorkbenchConfig config = new WorkbenchConfig();
      config.firecloud = new WorkbenchConfig.FireCloudConfig();
      config.firecloud.clusterMaxAgeDays = (int) MAX_AGE.toDays();
      config.firecloud.clusterIdleMaxAgeDays = (int) IDLE_MAX_AGE.toDays();
      return config;
    }
  }

  @Autowired ClusterApi clusterApi;
  @Autowired OfflineClusterController controller;
  private int projectIdIndex = 0;

  @Before
  public void setUp() {
    CLOCK.setInstant(NOW);
    projectIdIndex = 0;
  }

  private Cluster clusterWithAge(Duration age) {
    return clusterWithAgeAndIdle(age, Duration.ZERO);
  }

  private Cluster clusterWithAgeAndIdle(Duration age, Duration idleTime) {
    // There should only be one cluster per project, so increment an index for
    // each cluster created per test.
    return new Cluster()
        .clusterName("all-of-us")
        .googleProject(String.format("proj-%d", projectIdIndex++))
        .status(ClusterStatus.RUNNING)
        .createdDate(NOW.minus(age).toString())
        .dateAccessed(NOW.minus(idleTime).toString());
  }

  private List<ListClusterResponse> toListClusterResponseList(List<Cluster> clusters) {
    return clusters.stream()
        .map(
            c ->
                // TODO: this is fragile.  will need a line for each field we want to test
                new ListClusterResponse()
                    .clusterName(c.getClusterName())
                    .googleProject(c.getGoogleProject())
                    .status(c.getStatus())
                    .createdDate(c.getCreatedDate())
                    .dateAccessed(c.getDateAccessed()))
        .collect(Collectors.toList());
  }

  private void stubClusters(List<Cluster> clusters) throws Exception {
    when(clusterApi.listClusters(any(), any())).thenReturn(toListClusterResponseList(clusters));

    for (Cluster cluster : clusters) {
      when(clusterApi.getCluster(cluster.getGoogleProject(), cluster.getClusterName()))
          .thenReturn(cluster);
    }
  }

  @Test
  public void testCheckClustersNoResults() throws Exception {
    stubClusters(ImmutableList.of());
    assertThat(controller.checkClusters().getBody().getClusterDeletionCount()).isEqualTo(0);

    verify(clusterApi, never()).deleteCluster(any(), any());
  }

  @Test
  public void testCheckClustersActiveCluster() throws Exception {
    stubClusters(ImmutableList.of(clusterWithAge(Duration.ofHours(10))));
    assertThat(controller.checkClusters().getBody().getClusterDeletionCount()).isEqualTo(0);

    verify(clusterApi, never()).deleteCluster(any(), any());
  }

  @Test
  public void testCheckClustersActiveTooOld() throws Exception {
    stubClusters(ImmutableList.of(clusterWithAge(MAX_AGE.plusMinutes(5))));
    assertThat(controller.checkClusters().getBody().getClusterDeletionCount()).isEqualTo(1);

    verify(clusterApi, times(1)).deleteCluster(any(), any());
  }

  @Test
  public void testCheckClustersIdleYoung() throws Exception {
    // Running for under the IDLE_MAX_AGE, idle for 10 hours
    stubClusters(
        ImmutableList.of(
            clusterWithAgeAndIdle(IDLE_MAX_AGE.minusMinutes(10), Duration.ofHours(10))));
    assertThat(controller.checkClusters().getBody().getClusterDeletionCount()).isEqualTo(0);

    verify(clusterApi, never()).deleteCluster(any(), any());
  }

  @Test
  public void testCheckClustersIdleOld() throws Exception {
    // Running for >IDLE_MAX_AGE, idle for 10 hours
    stubClusters(
        ImmutableList.of(
            clusterWithAgeAndIdle(IDLE_MAX_AGE.plusMinutes(15), Duration.ofHours(10))));
    assertThat(controller.checkClusters().getBody().getClusterDeletionCount()).isEqualTo(1);

    verify(clusterApi, times(1)).deleteCluster(any(), any());
  }

  @Test
  public void testCheckClustersBrieflyIdleOld() throws Exception {
    // Running for >IDLE_MAX_AGE, idle for only 15 minutes
    stubClusters(
        ImmutableList.of(
            clusterWithAgeAndIdle(IDLE_MAX_AGE.plusMinutes(15), Duration.ofMinutes(15))));
    assertThat(controller.checkClusters().getBody().getClusterDeletionCount()).isEqualTo(0);

    verify(clusterApi, never()).deleteCluster(any(), any());
  }

  @Test
  public void testCheckClustersOtherStatusFiltered() throws Exception {
    stubClusters(
        ImmutableList.of(clusterWithAge(MAX_AGE.plusDays(10)).status(ClusterStatus.DELETING)));
    assertThat(controller.checkClusters().getBody().getClusterDeletionCount()).isEqualTo(0);

    verify(clusterApi, never()).deleteCluster(any(), any());
  }
}
