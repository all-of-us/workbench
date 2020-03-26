package org.pmiops.workbench.actionaudit.auditors;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.pmiops.workbench.actionaudit.ActionAuditEvent;
import org.pmiops.workbench.actionaudit.ActionAuditService;
import org.pmiops.workbench.actionaudit.ActionType;
import org.pmiops.workbench.actionaudit.TargetType;
import org.pmiops.workbench.actionaudit.targetproperties.AclTargetProperty;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.utils.WorkspaceMapper;
import org.pmiops.workbench.utils.WorkspaceMapperImpl;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.workspaces.ManualWorkspaceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class WorkspaceAuditorTest {

  private static final long WORKSPACE_1_DB_ID = 101L;
  private static final long Y2K_EPOCH_MILLIS =
      Instant.parse("2000-01-01T00:00:00.00Z").toEpochMilli();
  private static final long REMOVED_USER_ID = 301L;
  private static final long ADDED_USER_ID = 401L;

  private Workspace workspace1;
  private Workspace workspace2;
  private DbUser user1;
  private DbWorkspace dbWorkspace1;
  private DbWorkspace dbWorkspace2;

  @Autowired private WorkspaceAuditor workspaceAuditor;
  @Autowired private WorkspaceMapper workspaceMapper;
  @Autowired private ManualWorkspaceMapper manualWorkspaceMapper;
  @MockBean private Provider<DbUser> mockUserProvider;
  @MockBean private ActionAuditService mockActionAuditService;

  @Captor private ArgumentCaptor<Collection<ActionAuditEvent>> eventCollectionCaptor;
  @Captor private ArgumentCaptor<ActionAuditEvent> eventCaptor;

  @TestConfiguration
  @Import({
    WorkspaceAuditorImpl.class,
    WorkspaceMapperImpl.class,
    ManualWorkspaceMapper.class,
    CommonMappers.class,
    ActionAuditTestConfig.class
  })
  static class Config {
    @Bean
    WorkbenchConfig workbenchConfig() {
      WorkbenchConfig workbenchConfig = new WorkbenchConfig();
      workbenchConfig.featureFlags = new WorkbenchConfig.FeatureFlagsConfig();
      workbenchConfig.featureFlags.enableBillingLockout = false;
      return workbenchConfig;
    }
  }

  @Before
  public void setUp() {
    final ResearchPurpose researchPurpose1 = new ResearchPurpose();
    researchPurpose1.setIntendedStudy("stubbed toes");
    researchPurpose1.setAdditionalNotes("I really like the cloud.");
    final long now = System.currentTimeMillis();

    workspace1 = new Workspace();
    workspace1.setName("DbWorkspace 1");
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

    dbWorkspace1 = manualWorkspaceMapper.toDbWorkspace(workspace1);
    dbWorkspace1.setWorkspaceId(WORKSPACE_1_DB_ID);
    dbWorkspace1.setLastAccessedTime(new Timestamp(now));
    dbWorkspace1.setLastModifiedTime(new Timestamp(now));
    dbWorkspace1.setCreationTime(new Timestamp(now));

    dbWorkspace2 = new DbWorkspace();
    dbWorkspace2.setWorkspaceId(201L);
    dbWorkspace2.setPublished(false);
    dbWorkspace2.setLastModifiedTime(new Timestamp(now));
    dbWorkspace2.setCreationTime(new Timestamp(now));
    dbWorkspace2.setCreator(user1);

    workspace2 = workspaceMapper.toApiWorkspace(dbWorkspace2, null);

    // By default, have the mock user provider return the test-config default user.
    doReturn(ActionAuditTestConfig.getUser()).when(mockUserProvider).get();
  }

  @Test
  public void testFiresCreateWorkspaceEvents() {
    workspaceAuditor.fireCreateAction(workspace1, WORKSPACE_1_DB_ID);
    verify(mockActionAuditService).send(eventCollectionCaptor.capture());
    Collection<ActionAuditEvent> eventsSent = eventCollectionCaptor.getValue();
    assertThat(eventsSent).hasSize(19);
    Optional<ActionAuditEvent> firstEvent = eventsSent.stream().findFirst();
    assertThat(firstEvent.isPresent()).isTrue();
    assertThat(firstEvent.map(ActionAuditEvent::getActionType).orElse(null))
        .isEqualTo(ActionType.CREATE);
    assertThat(
            eventsSent.stream()
                .map(ActionAuditEvent::getActionType)
                .collect(Collectors.toSet())
                .size())
        .isEqualTo(1);
  }

  @Test
  public void testFiresDeleteWorkspaceEvent() {
    workspaceAuditor.fireDeleteAction(dbWorkspace1);
    verify(mockActionAuditService).send(eventCaptor.capture());
    final ActionAuditEvent eventSent = eventCaptor.getValue();
    assertThat(eventSent.getActionType()).isEqualTo(ActionType.DELETE);
    assertThat(eventSent.getTimestamp()).isEqualTo(ActionAuditTestConfig.INSTANT.toEpochMilli());
  }

  @Test
  public void testFiresDuplicateEvent() {
    workspaceAuditor.fireDuplicateAction(
        dbWorkspace1.getWorkspaceId(), dbWorkspace2.getWorkspaceId(), workspace2);
    verify(mockActionAuditService).send(eventCollectionCaptor.capture());
    final Collection<ActionAuditEvent> eventsSent = eventCollectionCaptor.getValue();
    assertThat(eventsSent).hasSize(13);

    // need same actionId for all events
    assertThat(eventsSent.stream().map(ActionAuditEvent::getActionId).distinct().count())
        .isEqualTo(1);

    assertThat(
            eventsSent.stream()
                .map(ActionAuditEvent::getTargetType)
                .allMatch(t -> t.equals(TargetType.WORKSPACE)))
        .isTrue();

    ImmutableSet<ActionType> expectedActionTypes =
        ImmutableSet.of(ActionType.DUPLICATE_FROM, ActionType.DUPLICATE_TO);
    ImmutableSet<ActionType> actualActionTypes =
        eventsSent.stream()
            .map(ActionAuditEvent::getActionType)
            .collect(ImmutableSet.toImmutableSet());
    assertThat(actualActionTypes).containsExactlyElementsIn(expectedActionTypes);
  }

  @Test
  public void testFiresCollaborateAction() {
    final ImmutableMap<Long, String> aclsByUserId =
        ImmutableMap.of(
            ActionAuditTestConfig.ADMINISTRATOR_USER_ID,
            WorkspaceAccessLevel.OWNER.toString(),
            REMOVED_USER_ID,
            WorkspaceAccessLevel.NO_ACCESS.toString(),
            ADDED_USER_ID,
            WorkspaceAccessLevel.READER.toString());
    workspaceAuditor.fireCollaborateAction(dbWorkspace1.getWorkspaceId(), aclsByUserId);
    verify(mockActionAuditService).send(eventCollectionCaptor.capture());
    Collection<ActionAuditEvent> eventsSent = eventCollectionCaptor.getValue();
    assertThat(eventsSent).hasSize(4);

    Map<String, Long> countByTargetType =
        eventsSent.stream()
            .collect(
                Collectors.groupingBy(e -> e.getTargetType().toString(), Collectors.counting()));

    assertThat(countByTargetType.get(TargetType.WORKSPACE.toString())).isEqualTo(1);
    assertThat(countByTargetType.get(TargetType.USER.toString())).isEqualTo(3);

    Optional<String> targetPropertyMaybe =
        eventsSent.stream()
            .filter(e -> e.getTargetType() == TargetType.USER)
            .findFirst()
            .flatMap(e -> Optional.ofNullable(e.getTargetPropertyMaybe()));

    assertThat(targetPropertyMaybe.isPresent()).isTrue();
    assertThat(targetPropertyMaybe.get()).isEqualTo(AclTargetProperty.ACCESS_LEVEL.toString());

    // need same actionId for all events
    assertThat(eventsSent.stream().map(ActionAuditEvent::getActionId).distinct().count())
        .isEqualTo(1);

    Optional<ActionAuditEvent> readerEventMaybe =
        eventsSent.stream()
            .filter(
                e ->
                    e.getTargetType() == TargetType.USER
                        && e.getTargetIdMaybe() != null
                        && e.getTargetIdMaybe().equals(ADDED_USER_ID))
            .findFirst();
    assertThat(readerEventMaybe.isPresent()).isTrue();
    assertThat(readerEventMaybe.get().getTargetPropertyMaybe()).isNotNull();
    assertThat(readerEventMaybe.get().getTargetPropertyMaybe())
        .isEqualTo(AclTargetProperty.ACCESS_LEVEL.toString());
    assertThat(readerEventMaybe.get().getNewValueMaybe())
        .isEqualTo(WorkspaceAccessLevel.READER.toString());
    assertThat(readerEventMaybe.get().getPreviousValueMaybe()).isNull();
  }

  @Test
  public void testCollaborateWithEmptyMapDoesNothing() {
    workspaceAuditor.fireCollaborateAction(WORKSPACE_1_DB_ID, Collections.emptyMap());
    verifyZeroInteractions(mockActionAuditService);
  }

  @Test
  public void testDoesNotThrowWhenUserProviderFails() {
    doReturn(null).when(mockUserProvider).get();
    workspaceAuditor.fireDeleteAction(dbWorkspace1);
  }

  @Test
  public void testFireEditAction_sendsNoEventsForSameWorkspace() {
    workspaceAuditor.fireEditAction(workspace1, workspace1, dbWorkspace1.getWorkspaceId());
    verify(mockActionAuditService).send(eventCollectionCaptor.capture());
    assertThat(eventCollectionCaptor.getValue()).isEmpty();
  }

  @Test
  public void testFireEditAction_sendsChangedProperties() {
    final ResearchPurpose editedResearchPurpose =
        new ResearchPurpose()
            .intendedStudy("stubbed toes")
            .additionalNotes("I really like the cloud.")
            .anticipatedFindings("I want to find my keys.")
            .controlSet(true);

    Workspace editedWorkspace =
        new Workspace()
            .name("New name")
            .id("fc-id-1")
            .namespace("aou-rw-local1-c4be869a")
            .creator("user@fake-research-aou.org")
            .cdrVersionId("1")
            .researchPurpose(editedResearchPurpose)
            .creationTime(Y2K_EPOCH_MILLIS)
            .lastModifiedTime(Y2K_EPOCH_MILLIS)
            .etag("etag_1")
            .dataAccessLevel(DataAccessLevel.REGISTERED)
            .published(false);

    workspaceAuditor.fireEditAction(workspace1, editedWorkspace, dbWorkspace1.getWorkspaceId());
    verify(mockActionAuditService).send(eventCollectionCaptor.capture());

    assertThat(eventCollectionCaptor.getValue()).hasSize(3);
  }
}
