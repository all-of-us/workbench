package org.pmiops.workbench.audit.adapters;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.pmiops.workbench.audit.ActionAuditEvent;
import org.pmiops.workbench.audit.ActionAuditService;
import org.pmiops.workbench.audit.ActionType;
import org.pmiops.workbench.audit.TargetType;
import org.pmiops.workbench.audit.targetproperties.AclTargetProperty;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.workspaces.WorkspaceConversionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class WorkspaceAuditAdapterServiceTest {

  private static final long WORKSPACE_1_DB_ID = 101L;
  public static final long Y2K_EPOCH_MILLIS = Instant.parse("2000-01-01T00:00:00.00Z").toEpochMilli();
  public static final long REMOVED_USER_ID = 301L;
  public static final long ADDED_USER_ID = 401L;
  private WorkspaceAuditAdapterService workspaceAuditAdapterService;
  private Workspace workspace1;
  private User user1;
  private org.pmiops.workbench.db.model.Workspace dbWorkspace1;
  private org.pmiops.workbench.db.model.Workspace dbWorkspace2;

  @Mock private Provider<User> mockUserProvider;
  @Mock private FakeClock mockClock;
  @Mock private ActionAuditService mockActionAuditService;

  @Captor private ArgumentCaptor<Collection<ActionAuditEvent>> eventListCaptor;
  @Captor private ArgumentCaptor<ActionAuditEvent> eventCaptor;

  @TestConfiguration
  @MockBean(value = {ActionAuditService.class})
  static class Configuration {}

  @Before
  public void setUp() {
    user1 = new User();
    user1.setUserId(101L);
    user1.setEmail("fflinstone@slate.com");
    user1.setGivenName("Fred");
    user1.setFamilyName("Flintstone");
    doReturn(user1).when(mockUserProvider).get();
    workspaceAuditAdapterService =
        new WorkspaceAuditAdapterServiceImpl(mockUserProvider, mockActionAuditService, mockClock);

    final ResearchPurpose researchPurpose1 = new ResearchPurpose();
    researchPurpose1.setIntendedStudy("stubbed toes");
    researchPurpose1.setAdditionalNotes("I really like the cloud.");
    final long now = System.currentTimeMillis();

    workspace1 = new Workspace();
    workspace1.setName("Workspace 1");
    workspace1.setId("fc-id-1");
    workspace1.setNamespace("aou-rw-local1-c4be869a");
    workspace1.setCreator("user@fake-research-aou.org");
    workspace1.setCdrVersionId("1");
    workspace1.setResearchPurpose(researchPurpose1);
    workspace1.setCreationTime(now);
    workspace1.setLastModifiedTime(now);
    workspace1.setEtag("etag_1");
    workspace1.setDataAccessLevel(DataAccessLevel.REGISTERED);
    workspace1.setPublished(false);

    dbWorkspace1 = WorkspaceConversionUtils.toDbWorkspace(workspace1);
    dbWorkspace1.setWorkspaceId(WORKSPACE_1_DB_ID);
    dbWorkspace1.setLastAccessedTime(new Timestamp(now));
    dbWorkspace1.setLastModifiedTime(new Timestamp(now));
    dbWorkspace1.setCreationTime(new Timestamp(now));

    dbWorkspace2 = new org.pmiops.workbench.db.model.Workspace();
    dbWorkspace2.setWorkspaceId(201L);
    dbWorkspace2.setPublished(false);
    dbWorkspace2.setLastModifiedTime(new Timestamp(now));
    dbWorkspace2.setCreationTime(new Timestamp(now));
    dbWorkspace2.setCreator(user1);

    doReturn(Y2K_EPOCH_MILLIS)
        .when(mockClock)
        .millis();
  }

  @Test
  public void testFiresCreateWorkspaceEvents() {
    workspaceAuditAdapterService.fireCreateAction(workspace1, WORKSPACE_1_DB_ID);
    verify(mockActionAuditService).send(eventListCaptor.capture());
    Collection<ActionAuditEvent> eventsSent = eventListCaptor.getValue();
    assertThat(eventsSent.size()).isEqualTo(6);
    Optional<ActionAuditEvent> firstEvent = eventsSent.stream().findFirst();
    assertThat(firstEvent.isPresent()).isTrue();
    assertThat(firstEvent.get().actionType()).isEqualTo(ActionType.CREATE);
    assertThat(
            eventsSent.stream()
                .map(ActionAuditEvent::actionType)
                .collect(Collectors.toSet())
                .size())
        .isEqualTo(1);
  }

  @Test
  public void testFirestNoEventsForNullWorkspace() {
    workspaceAuditAdapterService.fireCreateAction(null, WORKSPACE_1_DB_ID);
    verify(mockActionAuditService).send(eventListCaptor.capture());
    Collection<ActionAuditEvent> eventsSent = eventListCaptor.getValue();
    assertThat(eventsSent).isEmpty();
  }

  @Test
  public void testFiresDeleteWorkspaceEvent() {
    workspaceAuditAdapterService.fireDeleteAction(dbWorkspace1);
    verify(mockActionAuditService).send(eventCaptor.capture());
    final ActionAuditEvent eventSent = eventCaptor.getValue();
    assertThat(eventSent.actionType()).isEqualTo(ActionType.DELETE);
    assertThat(eventSent.timestamp()).isEqualTo(Y2K_EPOCH_MILLIS);
  }

  @Test
  public void testFiresDuplicateEvent() {
    workspaceAuditAdapterService.fireDuplicateAction(dbWorkspace1, dbWorkspace2);
    verify(mockActionAuditService).send(eventListCaptor.capture());
    final Collection<ActionAuditEvent> eventsSent = eventListCaptor.getValue();
    assertThat(eventsSent).hasSize(2);

    // need same actionId for all events
    assertThat(eventsSent.stream()
          .map(ActionAuditEvent::actionId)
          .distinct()
          .count())
        .isEqualTo(1);

    assertThat(eventsSent.stream()
        .map(ActionAuditEvent::targetType)
        .allMatch(t -> t.equals(TargetType.WORKSPACE)))
        .isTrue();

    ImmutableSet<ActionType> expectedActionTypes = ImmutableSet.of(ActionType.DUPLICATE_FROM,
        ActionType.DUPLICATE_TO);
    ImmutableSet<ActionType> actualActionTypes = eventsSent.stream()
        .map(ActionAuditEvent::actionType)
        .collect(ImmutableSet.toImmutableSet());
    assertThat(actualActionTypes).containsExactlyElementsIn(expectedActionTypes);
  }

  @Test
  public void testFiresCollaborateAction() {
    final ImmutableMap<Long, String> aclsByUserId = ImmutableMap.of(
        user1.getUserId(), WorkspaceAccessLevel.OWNER.toString(),
        REMOVED_USER_ID, WorkspaceAccessLevel.NO_ACCESS.toString(),
        ADDED_USER_ID, WorkspaceAccessLevel.READER.toString());
    workspaceAuditAdapterService.fireCollaborateAction(
        dbWorkspace1.getWorkspaceId(),
        aclsByUserId);
    verify(mockActionAuditService).send(eventListCaptor.capture());
    Collection<ActionAuditEvent> eventsSent = eventListCaptor.getValue();
    assertThat(eventsSent).hasSize(4);

    Map<String, Long> countByTargetType = eventsSent.stream()
        .collect(Collectors.groupingBy(e -> e.targetType().toString(), Collectors.counting()));

    assertThat(countByTargetType.get(TargetType.WORKSPACE.toString()))
        .isEqualTo(1);
    assertThat(countByTargetType.get(TargetType.USER.toString()))
        .isEqualTo(3);

    Optional<String> targetPropertyMaybe = eventsSent.stream()
        .filter(e -> e.targetType() == TargetType.USER)
        .findFirst()
        .flatMap(ActionAuditEvent::targetProperty);

    assertThat(targetPropertyMaybe.isPresent()).isTrue();
    assertThat(targetPropertyMaybe.get())
        .isEqualTo(AclTargetProperty.ACCESS_LEVEL.toString());

    // need same actionId for all events
    assertThat(eventsSent.stream()
        .map(ActionAuditEvent::actionId)
        .distinct()
        .count())
        .isEqualTo(1);

    Optional<ActionAuditEvent> readerEventMaybe = eventsSent.stream()
        .filter(e -> e.targetType() == TargetType.USER
          && e.targetId().isPresent()
          && e.targetId().get().equals(ADDED_USER_ID))
        .findFirst();
    assertThat(readerEventMaybe.isPresent()).isTrue();
    assertThat(readerEventMaybe.get().targetProperty().isPresent()).isTrue();
    assertThat(readerEventMaybe.get().targetProperty().get()).isEqualTo(AclTargetProperty.ACCESS_LEVEL.toString());
    assertThat(readerEventMaybe.get().newValue().get()).isEqualTo(WorkspaceAccessLevel.READER.toString());
    assertThat(readerEventMaybe.get().previousValue().isPresent()).isFalse();
  }

  @Test
  public void testCollaborateWithEmptyMapDoesNothing() {
    workspaceAuditAdapterService.fireCollaborateAction(WORKSPACE_1_DB_ID, Collections.emptyMap());
    verifyZeroInteractions(mockActionAuditService);
  }

  @Test
  public void testDoesNotThrowWhenMissingRequiredFields() {
    workspace1.setResearchPurpose(null); // programming error
    workspaceAuditAdapterService.fireCreateAction(workspace1, WORKSPACE_1_DB_ID);
  }

  @Test
  public void testDoesNotThrowWhenUserProviderFails() {
    doReturn(null).when(mockUserProvider).get();
    workspaceAuditAdapterService.fireDeleteAction(dbWorkspace1);
  }
}
