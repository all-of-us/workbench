package org.pmiops.workbench.actionaudit.auditors;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
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
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.test.FakeClock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class AuthDomainAuditorTest {

  private static final long USER_ID = 101L;
  private static final long ADMINISTRATOR_USER_ID = 222L;
  private static final String ADMINISTRATOR_EMAIL = "admin@aou.biz";
  private static final Instant INSTANT = Instant.parse("2000-01-01T00:00:00.00Z");
  private static final String ACTION_ID = "9095d2f9-8db2-46c3-8f8e-4f90a62b457f";

  @Autowired private ActionAuditService mockActionAuditService;

  @Captor private ArgumentCaptor<ActionAuditEvent> eventCaptor;

  @Autowired private AuthDomainAuditor authDomainAuditAdapter;

  @TestConfiguration
  @Import({AuthDomainAuditorImpl.class})
  @MockBean({ActionAuditService.class})
  static class Config {
    @Bean(name = "ACTION_ID")
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public String getActionId() {
      return ACTION_ID;
    }

    @Bean
    public DbUser getUser() {
      final DbUser administrator = new DbUser();
      administrator.setUserId(ADMINISTRATOR_USER_ID);
      administrator.setUserName(ADMINISTRATOR_EMAIL);
      return administrator;
    }

    @Bean
    public Clock getClock() {
      return new FakeClock(INSTANT);
    }
  }

  @Test
  public void testFires() {
    authDomainAuditAdapter.fireSetAccountDisabledStatus(USER_ID, true, false);
    verify(mockActionAuditService).send(eventCaptor.capture());
    final ActionAuditEvent event = eventCaptor.getValue();

    assertThat(event.getTimestamp()).isEqualTo(INSTANT.toEpochMilli());
    assertThat(event.getAgentType()).isEqualTo(AgentType.ADMINISTRATOR);
    assertThat(event.getAgentId()).isEqualTo(ADMINISTRATOR_USER_ID);
    assertThat(event.getAgentEmailMaybe()).contains(ADMINISTRATOR_EMAIL);
    assertThat(event.getActionId()).contains(ACTION_ID);
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
