package org.pmiops.workbench.exfiltration;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.JpaFakeDateTimeConfiguration;
import org.pmiops.workbench.actionaudit.auditors.EgressEventAuditor;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchConfig.EgressAlertRemediationPolicy;
import org.pmiops.workbench.config.WorkbenchConfig.EgressAlertRemediationPolicy.Escalation;
import org.pmiops.workbench.config.WorkbenchConfig.EgressAlertRemediationPolicy.Escalation.DisableUser;
import org.pmiops.workbench.config.WorkbenchConfig.EgressAlertRemediationPolicy.Escalation.SuspendCompute;
import org.pmiops.workbench.db.dao.EgressEventDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbEgressEvent;
import org.pmiops.workbench.db.model.DbEgressEvent.EgressEventStatus;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.mail.MailService.EgressRemediationAction;
import org.pmiops.workbench.notebooks.LeonardoNotebooksClient;
import org.pmiops.workbench.test.FakeClock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;

@DataJpaTest
public class EgressRemediationServiceTest {

  private static final String USER_EMAIL = "asdf@fake-research-aou.org";
  private static WorkbenchConfig workbenchConfig;

  @MockBean private UserService mockUserService;
  @MockBean private LeonardoNotebooksClient mockLeonardoNotebooksClient;
  @MockBean private EgressEventAuditor mockEgressEventAuditor;
  @MockBean private MailService mockMailService;

  @Autowired private FakeClock fakeClock;
  @Autowired private WorkspaceDao workspaceDao;
  @Autowired private EgressEventDao egressEventDao;
  @Autowired private UserDao userDao;

  @Autowired private EgressRemediationService egressRemediationService;

  private long userId;
  private DbWorkspace dbWorkspace;
  private DbWorkspace dbWorkspace2;

  @TestConfiguration
  @Import({
    EgressRemediationService.class,
    FakeClockConfiguration.class,
    JpaFakeDateTimeConfiguration.class,
  })
  static class Configuration {

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    WorkbenchConfig getWorkbenchConfig() {
      return workbenchConfig;
    }
  }

  @BeforeEach
  public void setUp() {
    workbenchConfig = WorkbenchConfig.createEmptyConfig();
    workbenchConfig.egressAlertRemediationPolicy = new EgressAlertRemediationPolicy();

    DbUser dbUser = new DbUser();
    dbUser.setUsername(USER_EMAIL);
    userId = userDao.save(dbUser).getUserId();

    dbWorkspace = new DbWorkspace();
    dbWorkspace.setWorkspaceNamespace("ns");
    dbWorkspace.setGoogleProject("proj");
    dbWorkspace = workspaceDao.save(dbWorkspace);

    dbWorkspace2 = new DbWorkspace();
    dbWorkspace2.setWorkspaceNamespace("ns2");
    dbWorkspace2.setGoogleProject("proj2");
    dbWorkspace2 = workspaceDao.save(dbWorkspace2);

    // Ideally we would use UserService's implementation directly, but the interface is too
    // complicated. Instead, just provide a minimal fake implementation of the relevant methods.
    when(mockUserService.updateUserWithRetries(any(), any(), any()))
        .then(
            invocation -> {
              Function<DbUser, DbUser> update = invocation.getArgument(0);
              DbUser u = invocation.getArgument(1);
              return userDao.save(update.apply(u));
            });
    when(mockUserService.setDisabledStatus(any(), anyBoolean()))
        .then(
            invocation -> {
              long userId = invocation.getArgument(0);
              boolean disabled = invocation.getArgument(1);
              DbUser u = userDao.findUserByUserId(userId);
              u.setDisabled(disabled);
              return userDao.save(u);
            });
  }

  @AfterEach
  public void tearDown() {
    egressEventDao.deleteAll();
    workspaceDao.deleteAll();
    userDao.deleteAll();
  }

  @Test
  public void testRemediateEgressEvent_notFound() {
    assertThrows(
        NotFoundException.class, () -> egressRemediationService.remediateEgressEvent(123L));
  }

  @Test
  public void testRemediateEgressEvent_alreadyRemediated() {
    long eventId = saveNewEvent(newEvent().setStatus(EgressEventStatus.REMEDIATED));
    egressRemediationService.remediateEgressEvent(eventId);

    assertThat(getDbUser().getDisabled()).isFalse();
    assertComputeNotSuspended();
    verifyZeroInteractions(mockLeonardoNotebooksClient);
  }

  @Test
  public void testRemediateEgressEvent_emptyPolicy() {
    workbenchConfig.egressAlertRemediationPolicy = null;

    egressRemediationService.remediateEgressEvent(saveNewEvent());

    assertThat(getDbUser().getDisabled()).isFalse();
    assertComputeNotSuspended();
    verifyZeroInteractions(mockLeonardoNotebooksClient);
  }

  @Test
  public void testRemediateEgressEvent_noMatchingEscalationPolicy() {
    workbenchConfig.egressAlertRemediationPolicy.escalations =
        ImmutableList.of(disableUserAfter(10));

    egressRemediationService.remediateEgressEvent(saveNewEvent());

    assertThat(getDbUser().getDisabled()).isFalse();
    assertComputeNotSuspended();
    verifyZeroInteractions(mockLeonardoNotebooksClient);
  }

  @Test
  public void testRemediateEgressEvent_firstIncident() {
    workbenchConfig.egressAlertRemediationPolicy.escalations =
        ImmutableList.of(suspendComputeAfter(1, Duration.ofMinutes(1)), disableUserAfter(2));

    egressRemediationService.remediateEgressEvent(saveNewEvent());

    assertThat(getDbUser().getDisabled()).isFalse();
    assertComputeSuspended(Duration.ofMinutes(1));
  }

  @Test
  public void testRemediateEgressEvent_intermediateEscalation() {
    workbenchConfig.egressAlertRemediationPolicy.escalations =
        ImmutableList.of(
            suspendComputeAfter(1, Duration.ofMinutes(1)),
            suspendComputeAfter(2, Duration.ofMinutes(2)),
            disableUserAfter(3));

    saveOldEvents(Duration.ofDays(1));
    egressRemediationService.remediateEgressEvent(saveNewEvent());

    assertThat(getDbUser().getDisabled()).isFalse();
    assertComputeSuspended(Duration.ofMinutes(2));
  }

  @Test
  public void testRemediateEgressEvent_maxEscalation() {
    workbenchConfig.egressAlertRemediationPolicy.escalations =
        ImmutableList.of(
            suspendComputeAfter(1, Duration.ofMinutes(1)),
            suspendComputeAfter(2, Duration.ofMinutes(2)),
            disableUserAfter(3));

    // Create 10 older events on different days.
    saveOldEvents(
        IntStream.range(1, 10)
            .mapToObj(i -> Duration.ofDays(i))
            .collect(Collectors.toList())
            .toArray(new Duration[] {}));
    egressRemediationService.remediateEgressEvent(saveNewEvent());

    assertThat(getDbUser().getDisabled()).isTrue();
    assertComputeNotSuspended();
  }

  @Test
  public void testRemediateEgressEvent_simpleIncidentMerge() {
    workbenchConfig.egressAlertRemediationPolicy = suspendXMinutesOnXIncidentsPolicy();

    // Two events within an hour of each-other should merge
    saveOldEvents(Duration.ofHours(22), Duration.ofHours(23));

    egressRemediationService.remediateEgressEvent(saveNewEvent());

    // 2 "incidents" == 2 minute suspension
    assertComputeSuspended(Duration.ofMinutes(2));
  }

  @Test
  public void testRemediateEgressEvent_incidentIncludesActiveEvent() {
    workbenchConfig.egressAlertRemediationPolicy = suspendXMinutesOnXIncidentsPolicy();

    saveOldEvents(Duration.ofMinutes(1), Duration.ofMinutes(2));
    egressRemediationService.remediateEgressEvent(saveNewEvent());

    // All 3 events should merge into a single logical incident.
    assertComputeSuspended(Duration.ofMinutes(1));
  }

  @Test
  public void testRemediateEgressEvent_noIncidentMergeAcrossWorkspaces() {
    workbenchConfig.egressAlertRemediationPolicy = suspendXMinutesOnXIncidentsPolicy();

    saveOldEvents(
        oldEvent(Duration.ofHours(3)).setWorkspace(dbWorkspace),
        oldEvent(Duration.ofHours(3)).setWorkspace(dbWorkspace2),
        oldEvent(Duration.ofHours(4)).setWorkspace(dbWorkspace2));

    egressRemediationService.remediateEgressEvent(saveNewEvent());

    // The two events in workspace 2 merge, the other old event and new event do not merge.
    assertComputeSuspended(Duration.ofMinutes(3));
  }

  @Test
  public void testRemediateEgressEvent_noIncidentMergeMissingWorkspace() {
    workbenchConfig.egressAlertRemediationPolicy = suspendXMinutesOnXIncidentsPolicy();

    saveOldEvents(
        oldEvent(Duration.ofHours(3)).setWorkspace(dbWorkspace),
        oldEvent(Duration.ofHours(3)).setWorkspace(null),
        oldEvent(Duration.ofHours(3)).setWorkspace(null));

    egressRemediationService.remediateEgressEvent(saveNewEvent());

    // None of the events merge
    assertComputeSuspended(Duration.ofMinutes(4));
  }

  @Test
  public void testRemediateEgressEvent_incidentMergeIsNonAssociative() {
    workbenchConfig.egressAlertRemediationPolicy = suspendXMinutesOnXIncidentsPolicy();

    // events 1, 2 should be merged but 3 should not
    saveOldEvents(Duration.ofMinutes(300), Duration.ofMinutes(250), Duration.ofMinutes(200));
    egressRemediationService.remediateEgressEvent(saveNewEvent());

    // 1 merged old incident, 1 standalone, and the active event
    assertComputeSuspended(Duration.ofMinutes(3));
  }

  @Test
  public void testRemediateEgressEvent_incidentsExcludeFalsePostives() {
    workbenchConfig.egressAlertRemediationPolicy = suspendXMinutesOnXIncidentsPolicy();

    saveOldEvents(
        oldEvent(Duration.ofHours(3)).setStatus(EgressEventStatus.VERIFIED_FALSE_POSITIVE));

    egressRemediationService.remediateEgressEvent(saveNewEvent());

    // Only the active event is considered
    assertComputeSuspended(Duration.ofMinutes(1));
  }

  @Test
  public void testRemediateEgressEvent_suspendCompute() throws Exception {
    workbenchConfig.egressAlertRemediationPolicy.escalations =
        ImmutableList.of(suspendComputeAfter(1, Duration.ofMinutes(1)));

    long eventId = saveNewEvent();
    egressRemediationService.remediateEgressEvent(eventId);

    assertThat(getDbUser().getDisabled()).isFalse();
    assertComputeSuspended(Duration.ofMinutes(1));

    DbEgressEvent event = egressEventDao.findById(eventId).get();
    assertThat(event.getStatus()).isEqualTo(EgressEventStatus.REMEDIATED);

    verify(mockMailService)
        .sendEgressRemediationEmail(any(), eq(EgressRemediationAction.SUSPEND_COMPUTE));
  }

  @Test
  public void testRemediateEgressEvent_disableUser() throws Exception {
    workbenchConfig.egressAlertRemediationPolicy.escalations =
        ImmutableList.of(disableUserAfter(1));

    long eventId = saveNewEvent();
    egressRemediationService.remediateEgressEvent(eventId);

    assertThat(getDbUser().getDisabled()).isTrue();
    assertComputeNotSuspended();
    // The disable action also stops the user's runtimes
    verify(mockLeonardoNotebooksClient).stopAllUserRuntimesAsService(USER_EMAIL);

    DbEgressEvent event = egressEventDao.findById(eventId).get();
    assertThat(event.getStatus()).isEqualTo(EgressEventStatus.REMEDIATED);

    verify(mockMailService)
        .sendEgressRemediationEmail(any(), eq(EgressRemediationAction.DISABLE_USER));
  }

  @Test
  public void testRemediateEgressEvent_auditOnRemediation() {
    workbenchConfig.egressAlertRemediationPolicy.escalations =
        ImmutableList.of(suspendComputeAfter(1, Duration.ofMinutes(1)));

    egressRemediationService.remediateEgressEvent(saveNewEvent());
    verify(mockEgressEventAuditor).fireRemediateEgressEvent(any(), notNull());
  }

  @Test
  public void testRemediateEgressEvent_notify_skipIfRecentEvent() {
    workbenchConfig.egressAlertRemediationPolicy.escalations =
        ImmutableList.of(suspendComputeAfter(1, Duration.ofMinutes(1)));

    saveOldEvents(oldEvent(Duration.ofMinutes(30L)).setStatus(EgressEventStatus.REMEDIATED));
    egressRemediationService.remediateEgressEvent(saveNewEvent());

    verifyZeroInteractions(mockMailService);
  }

  @Test
  public void testRemediateEgressEvent_notify_sendIfRecentUnrelatedEvents() throws Exception {
    workbenchConfig.egressAlertRemediationPolicy.escalations =
        ImmutableList.of(suspendComputeAfter(1, Duration.ofMinutes(1)));

    saveOldEvents(
        oldEvent(Duration.ofMinutes(10L))
            .setStatus(EgressEventStatus.REMEDIATED)
            .setWorkspace(dbWorkspace2),
        oldEvent(Duration.ofHours(2L)).setStatus(EgressEventStatus.REMEDIATED));
    egressRemediationService.remediateEgressEvent(saveNewEvent());

    verify(mockMailService)
        .sendEgressRemediationEmail(any(), eq(EgressRemediationAction.SUSPEND_COMPUTE));
  }

  private void saveOldEvents(Duration... ages) {
    saveOldEvents(
        Arrays.stream(ages)
            .map(this::oldEvent)
            .collect(Collectors.toList())
            .toArray(new DbEgressEvent[] {}));
  }

  private void saveOldEvents(DbEgressEvent... events) {
    for (DbEgressEvent target : events) {
      saveEventAtCreationTime(target);
    }
  }

  private long saveNewEvent() {
    return saveNewEvent(newEvent());
  }

  private long saveNewEvent(DbEgressEvent e) {
    return saveEventAtCreationTime(e).getEgressEventId();
  }

  private DbEgressEvent saveEventAtCreationTime(DbEgressEvent target) {
    Instant originalTime = fakeClock.instant();
    fakeClock.setInstant(target.getCreationTime().toInstant());

    DbEgressEvent e = egressEventDao.save(target);
    fakeClock.setInstant(originalTime);
    return e;
  }

  private EgressAlertRemediationPolicy suspendXMinutesOnXIncidentsPolicy() {
    EgressAlertRemediationPolicy policy = new EgressAlertRemediationPolicy();
    policy.escalations =
        IntStream.range(1, 20)
            .mapToObj(i -> suspendComputeAfter(i, Duration.ofMinutes(i)))
            .collect(Collectors.toList());
    return policy;
  }

  private DbEgressEvent oldEvent(Duration age) {
    Timestamp creationTime = Timestamp.from(FakeClockConfiguration.NOW.toInstant().minus(age));
    return newEvent().setStatus(EgressEventStatus.REMEDIATED).setCreationTime(creationTime);
  }

  private DbEgressEvent newEvent() {
    return new DbEgressEvent()
        .setUser(getDbUser())
        .setWorkspace(dbWorkspace)
        .setCreationTime(FakeClockConfiguration.NOW)
        .setStatus(EgressEventStatus.PENDING);
  }

  private Escalation suspendComputeAfter(int afterIncidentCount, Duration duration) {
    Escalation e = new Escalation();
    e.afterIncidentCount = afterIncidentCount;
    e.suspendCompute = new SuspendCompute();
    e.suspendCompute.durationMinutes = duration.toMinutes();
    return e;
  }

  private Escalation disableUserAfter(int afterIncidentCount) {
    Escalation e = new Escalation();
    e.afterIncidentCount = afterIncidentCount;
    e.disableUser = new DisableUser();
    return e;
  }

  private void assertComputeSuspended(Duration d) {
    Duration suspendedFor =
        Duration.between(
            FakeClockConfiguration.NOW.toInstant(),
            getDbUser().getComputeSecuritySuspendedUntil().toInstant());
    assertThat(suspendedFor).isEqualTo(d);
    verify(mockLeonardoNotebooksClient).stopAllUserRuntimesAsService(USER_EMAIL);
  }

  private void assertComputeNotSuspended() {
    assertThat(getDbUser().getComputeSecuritySuspendedUntil()).isNull();
  }

  private DbUser getDbUser() {
    return userDao.findUserByUserId(userId);
  }
}
