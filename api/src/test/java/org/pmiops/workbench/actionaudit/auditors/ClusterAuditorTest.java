package org.pmiops.workbench.actionaudit.auditors;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableList;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.pmiops.workbench.actionaudit.ActionAuditEvent;
import org.pmiops.workbench.actionaudit.ActionAuditService;
import org.pmiops.workbench.actionaudit.ActionType;
import org.pmiops.workbench.db.model.DbUser;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class ClusterAuditorTest {
  private DbUser user1;
  private static final long Y2K_EPOCH_MILLIS =
      Instant.parse("2000-01-01T00:00:00.00Z").toEpochMilli();
  private static final String ACTION_ID = "58cbae08-447f-499f-95b9-7bdedc955f4d";
  private static final String BILLING_PROJECT_ID = "all-of-us-yjty";
  private static final List<String> CLUSTER_NAMES =
      ImmutableList.of("all-of-us-1", "all-of-us-2", "all-of-us-3");

  private ClusterAuditor clusterAuditor;

  @Captor private ArgumentCaptor<Collection<ActionAuditEvent>> eventCollectionCaptor;

  @Mock private Provider<String> mockActionIdProvider;
  @Mock private ActionAuditService mockActionAuditService;
  @Mock private Clock mockClock;
  @Mock private Provider<DbUser> mockUserProvider;

  @TestConfiguration
  @MockBean(value = {ActionAuditService.class})
  static class Configuration {}

  @Before
  public void setUp() {
    user1 = new DbUser();
    user1.setUserId(101L);
    user1.setUsername("fflinstone@slate.com");
    user1.setGivenName("Fred");
    user1.setFamilyName("Flintstone");
    doReturn(user1).when(mockUserProvider).get();
    clusterAuditor =
        new ClusterAuditorImpl(
            mockActionIdProvider, mockActionAuditService, mockClock, mockUserProvider);

    doReturn(Y2K_EPOCH_MILLIS).when(mockClock).millis();
    doReturn(ACTION_ID).when(mockActionIdProvider).get();
  }

  @Test
  public void testFireDeleteClustersInProject() {
    clusterAuditor.fireDeleteClustersInProject(BILLING_PROJECT_ID, CLUSTER_NAMES);
    verify(mockActionAuditService).send(eventCollectionCaptor.capture());
    Collection<ActionAuditEvent> eventsSent = eventCollectionCaptor.getValue();
    assertThat(eventsSent).hasSize(CLUSTER_NAMES.size());
    Optional<ActionAuditEvent> firstEvent = eventsSent.stream().findFirst();
    assertThat(firstEvent.isPresent()).isTrue();
    assertThat(firstEvent.map(ActionAuditEvent::getActionType).orElse(null))
        .isEqualTo(ActionType.DELETE);
    assertThat(firstEvent.map(ActionAuditEvent::getTargetPropertyMaybe).orElse(null))
        .isEqualTo(BILLING_PROJECT_ID);
    assertThat(firstEvent.map(ActionAuditEvent::getNewValueMaybe).orElse(null))
        .isEqualTo(CLUSTER_NAMES.get(0));
    assertThat(
            eventsSent.stream()
                .map(ActionAuditEvent::getActionType)
                .collect(Collectors.toSet())
                .size())
        .isEqualTo(1);
  }
}
