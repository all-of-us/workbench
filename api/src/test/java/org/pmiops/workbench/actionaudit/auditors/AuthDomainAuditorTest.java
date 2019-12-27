package org.pmiops.workbench.actionaudit.auditors;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.pmiops.workbench.actionaudit.ActionAuditEvent;
import org.pmiops.workbench.actionaudit.ActionAuditService;
import org.pmiops.workbench.actionaudit.ActionType;
import org.pmiops.workbench.actionaudit.AgentType;
import org.pmiops.workbench.actionaudit.TargetType;
import org.pmiops.workbench.actionaudit.targetproperties.AccountTargetProperty;
import org.pmiops.workbench.actionaudit.targetproperties.values.AccountDisabledStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class AuthDomainAuditorTest {

  private static final long USER_ID = 101L;

  @Autowired private AuthDomainAuditor authDomainAuditAdapter;
  @MockBean private ActionAuditService mockActionAuditService;
  @Captor private ArgumentCaptor<ActionAuditEvent> eventCaptor;

  @TestConfiguration
  @Import({AuthDomainAuditorImpl.class, ActionAuditTestConfig.class})
  static class Config {}

  @Test
  public void testFires() {
    authDomainAuditAdapter.fireSetAccountDisabledStatus(USER_ID, true, false);
    verify(mockActionAuditService).send(eventCaptor.capture());
    final ActionAuditEvent event = eventCaptor.getValue();

    assertThat(event.getTimestamp()).isEqualTo(ActionAuditTestConfig.INSTANT.toEpochMilli());
    assertThat(event.getAgentType()).isEqualTo(AgentType.ADMINISTRATOR);
    assertThat(event.getAgentId()).isEqualTo(ActionAuditTestConfig.ADMINISTRATOR_USER_ID);
    assertThat(event.getAgentEmailMaybe()).contains(ActionAuditTestConfig.ADMINISTRATOR_EMAIL);
    assertThat(event.getActionId()).contains(ActionAuditTestConfig.ACTION_ID);
    assertThat(event.getActionType()).isEqualTo(ActionType.EDIT);
    assertThat(event.getTargetType()).isEqualTo(TargetType.ACCOUNT);
    assertThat(event.getTargetPropertyMaybe())
        .isEqualTo(AccountTargetProperty.IS_ENABLED.getPropertyName());
    assertThat(event.getTargetIdMaybe()).isEqualTo(USER_ID);
    assertThat(event.getPreviousValueMaybe())
        .isEqualTo(AccountDisabledStatus.ENABLED.getValueName());
    assertThat(event.getNewValueMaybe()).isEqualTo(AccountDisabledStatus.DISABLED.getValueName());
  }
}
