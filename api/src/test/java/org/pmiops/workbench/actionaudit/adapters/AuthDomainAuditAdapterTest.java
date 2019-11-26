package org.pmiops.workbench.actionaudit.adapters;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import javax.inject.Provider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.pmiops.workbench.actionaudit.ActionAuditEvent;
import org.pmiops.workbench.actionaudit.ActionAuditService;
import org.pmiops.workbench.actionaudit.ActionAuditServiceImpl;
import org.pmiops.workbench.actionaudit.ActionType;
import org.pmiops.workbench.actionaudit.AgentType;
import org.pmiops.workbench.actionaudit.TargetType;
import org.pmiops.workbench.actionaudit.targetproperties.AccountTargetProperty;
import org.pmiops.workbench.actionaudit.targetproperties.AclTargetProperty;
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
public class AuthDomainAuditAdapterTest {

  private static final long USER_ID = 101L;
  private static final long ADMINISTRATOR_USER_ID = 222L;
  private static final String ADMINISTRATOR_EMAIL = "admin@aou.biz";
  private static final Instant INSTANT = Instant.parse("2000-01-01T00:00:00.00Z");
  private static final String ACTION_ID = "9095d2f9-8db2-46c3-8f8e-4f90a62b457f";
  @Mock private Provider<DbUser> mockUserProvider;
  @Mock private ActionAuditService mockActionAuditService;
  @Mock private Provider<String> mockActionIdProvider;

  @Captor
  private ArgumentCaptor<ActionAuditEvent> eventCaptor;

  private AuthDomainAuditAdapter authDomainAuditAdapter;

  @TestConfiguration
  private static class Config { }

  @Before
  public void setup() {
    Clock fakeClock = new FakeClock(INSTANT);

    final DbUser administrator = new DbUser();
    administrator.setUserId(ADMINISTRATOR_USER_ID);
    administrator.setEmail(ADMINISTRATOR_EMAIL);
    doReturn(administrator).when(mockUserProvider).get();

    doReturn(ACTION_ID).when(mockActionIdProvider).get();

    authDomainAuditAdapter = new AuthDomainAuditAdapterImpl(
        mockActionAuditService,
        fakeClock,
        mockUserProvider,
        mockActionIdProvider);
  }

  @Test
  public void testFires() {
    authDomainAuditAdapter.fireSetAccountEnabled(USER_ID, true, false);
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
    assertThat(event.getPreviousValueMaybe()).isEqualTo(Boolean.toString(false));
    assertThat(event.getNewValueMaybe()).isEqualTo(Boolean.toString(true));
  }
}
