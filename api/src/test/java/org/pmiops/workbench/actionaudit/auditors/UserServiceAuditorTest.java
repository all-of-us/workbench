package org.pmiops.workbench.actionaudit.auditors;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.pmiops.workbench.actionaudit.ActionAuditEvent;
import org.pmiops.workbench.actionaudit.ActionAuditService;
import org.pmiops.workbench.actionaudit.Agent;
import org.pmiops.workbench.actionaudit.AgentType;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.DataAccessLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class UserServiceAuditorTest {
  @TestConfiguration
  @Import({UserServiceAuditAdapterImpl.class, ActionAuditTestConfig.class})
  @MockBean(ActionAuditService.class)
  static class Configuration {}

  @Captor ArgumentCaptor<ActionAuditEvent> eventArg;

  @Autowired UserServiceAuditor userServiceAuditor;
  @Autowired ActionAuditService mockActionAuditService;

  private static DbUser createUser() {
    final DbUser user = new DbUser();
    user.setUserId(3L);
    user.setUsername("foo@gmail.com");
    return user;
  }

  @Test
  public void testFireUpdateDataAccessAction_userAgent() {
    DbUser user = createUser();
    userServiceAuditor.fireUpdateDataAccessAction(
        user, DataAccessLevel.UNREGISTERED, DataAccessLevel.REGISTERED, Agent.asUser(user));
    verify(mockActionAuditService).send(eventArg.capture());

    ActionAuditEvent event = eventArg.getValue();
    assertThat(event.getAgentType()).isEqualTo(AgentType.USER);
    assertThat(event.getAgentIdMaybe()).isEqualTo(user.getUserId());
    assertThat(event.getAgentEmailMaybe()).isEqualTo(user.getUsername());
  }

  @Test
  public void testFireUpdateDataAccessAction_systemAgent() {
    userServiceAuditor.fireUpdateDataAccessAction(
        createUser(), DataAccessLevel.UNREGISTERED, DataAccessLevel.REGISTERED, Agent.asSystem());
    verify(mockActionAuditService).send(eventArg.capture());

    ActionAuditEvent event = eventArg.getValue();
    assertThat(event.getAgentType()).isEqualTo(AgentType.SYSTEM);
    assertThat(event.getAgentIdMaybe()).isNull();
    assertThat(event.getAgentEmailMaybe()).isNull();
  }
}
