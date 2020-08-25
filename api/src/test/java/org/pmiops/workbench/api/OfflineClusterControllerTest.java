package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
import org.pmiops.workbench.config.WorkbenchConfig.FireCloudConfig;
import org.pmiops.workbench.leonardo.api.ClusterApi;
import org.pmiops.workbench.leonardo.model.Cluster;
import org.pmiops.workbench.leonardo.model.ClusterStatus;
import org.pmiops.workbench.leonardo.model.ListClusterResponse;
import org.pmiops.workbench.notebooks.NotebooksConfig;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.utils.ClusterMapper;
import org.pmiops.workbench.utils.ClusterMapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class OfflineClusterControllerTest {
  private static final Instant NOW = Instant.parse("1988-12-26T00:00:00Z");
  private static final FakeClock CLOCK = new FakeClock(NOW, ZoneId.systemDefault());
  private static final Duration MAX_AGE = Duration.ofDays(14);
  private static final Duration IDLE_MAX_AGE = Duration.ofDays(7);

  @TestConfiguration
  @Import({OfflineClusterController.class, ClusterMapperImpl.class})
  static class Configuration {

    @Bean
    Clock clock() {
      return CLOCK;
    }

    @Bean
    public WorkbenchConfig workbenchConfig() {
      WorkbenchConfig config = WorkbenchConfig.createEmptyConfig();
      config.firecloud = new FireCloudConfig();
      config.firecloud.clusterMaxAgeDays = (int) MAX_AGE.toDays();
      config.firecloud.clusterIdleMaxAgeDays = (int) IDLE_MAX_AGE.toDays();
      return config;
    }
  }

  @Qualifier(NotebooksConfig.SERVICE_CLUSTER_API)
  @MockBean
  private ClusterApi mockClusterApi;

  @Autowired private ClusterMapper clusterMapper;
  @Autowired private OfflineClusterController controller;

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
    return clusters.stream().map(clusterMapper::toListClusterResponse).collect(Collectors.toList());
  }

  private void stubClusters(List<Cluster> clusters) throws Exception {
    when(mockClusterApi.listClusters(any(), any())).thenReturn(toListClusterResponseList(clusters));

    for (Cluster cluster : clusters) {
      when(mockClusterApi.getCluster(cluster.getGoogleProject(), cluster.getClusterName()))
          .thenReturn(cluster);
    }
  }

  @Test
  public void testCheckClustersNoResults() throws Exception {
    stubClusters(ImmutableList.of());
    assertThat(controller.checkClusters().getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    verify(mockClusterApi, never()).deleteCluster(any(), any());
  }

  @Test
  public void testCheckClustersActiveCluster() throws Exception {
    stubClusters(ImmutableList.of(clusterWithAge(Duration.ofHours(10))));
    assertThat(controller.checkClusters().getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    verify(mockClusterApi, never()).deleteCluster(any(), any());
  }

  @Test
  public void testCheckClustersActiveTooOld() throws Exception {
    stubClusters(ImmutableList.of(clusterWithAge(MAX_AGE.plusMinutes(5))));
    assertThat(controller.checkClusters().getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    verify(mockClusterApi).deleteCluster(any(), any());
  }

  @Test
  public void testCheckClustersIdleYoung() throws Exception {
    // Running for under the IDLE_MAX_AGE, idle for 10 hours
    stubClusters(
        ImmutableList.of(
            clusterWithAgeAndIdle(IDLE_MAX_AGE.minusMinutes(10), Duration.ofHours(10))));
    assertThat(controller.checkClusters().getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    verify(mockClusterApi, never()).deleteCluster(any(), any());
  }

  @Test
  public void testCheckClustersIdleOld() throws Exception {
    // Running for >IDLE_MAX_AGE, idle for 10 hours
    stubClusters(
        ImmutableList.of(
            clusterWithAgeAndIdle(IDLE_MAX_AGE.plusMinutes(15), Duration.ofHours(10))));
    assertThat(controller.checkClusters().getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    verify(mockClusterApi).deleteCluster(any(), any());
  }

  @Test
  public void testCheckClustersBrieflyIdleOld() throws Exception {
    // Running for >IDLE_MAX_AGE, idle for only 15 minutes
    stubClusters(
        ImmutableList.of(
            clusterWithAgeAndIdle(IDLE_MAX_AGE.plusMinutes(15), Duration.ofMinutes(15))));
    assertThat(controller.checkClusters().getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    verify(mockClusterApi, never()).deleteCluster(any(), any());
  }

  @Test
  public void testCheckClustersOtherStatusFiltered() throws Exception {
    stubClusters(
        ImmutableList.of(clusterWithAge(MAX_AGE.plusDays(10)).status(ClusterStatus.DELETING)));
    assertThat(controller.checkClusters().getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    verify(mockClusterApi, never()).deleteCluster(any(), any());
  }
}
