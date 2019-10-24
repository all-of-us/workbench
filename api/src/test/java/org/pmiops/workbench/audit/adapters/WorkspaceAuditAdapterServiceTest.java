package org.pmiops.workbench.audit.adapters;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.sql.Timestamp;
import java.util.Collection;
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
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.workspaces.WorkspaceConversionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class WorkspaceAuditAdapterServiceTest {

  private static final long WORKSPACE_1_DB_ID = 101L;
  private WorkspaceAuditAdapterService workspaceAuditAdapterService;
  private Workspace workspace1;
  private User user1;
  private DbWorkspace dbWorkspace1;

  @Mock private Provider<User> mockUserProvider;
  @Autowired private ActionAuditService mockActionAuditService;
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
        new WorkspaceAuditAdapterServiceImpl(mockUserProvider, mockActionAuditService);

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
  public void testFiresDeleteWorkspaceEvents() {
    workspaceAuditAdapterService.fireDeleteAction(dbWorkspace1);
    verify(mockActionAuditService).send(eventCaptor.capture());
    final ActionAuditEvent eventSent = eventCaptor.getValue();
    assertThat(eventSent.actionType()).isEqualTo(ActionType.DELETE);
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
