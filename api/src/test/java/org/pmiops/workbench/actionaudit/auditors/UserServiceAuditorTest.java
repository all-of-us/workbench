package org.pmiops.workbench.actionaudit.auditors;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.actionaudit.ActionAuditEvent;
import org.pmiops.workbench.actionaudit.ActionAuditService;
import org.pmiops.workbench.actionaudit.AgentType;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.DataAccessLevel;
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
public class UserServiceAuditorTest {
  static DbUser USER = null;

  @TestConfiguration
  @Import({UserServiceAuditAdapterImpl.class})
  @MockBean(ActionAuditService.class)
  static class Configuration {
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public DbUser getUser() {
      return USER;
    }

    @Bean(name = "ACTION_ID")
    public String getActionId() {
      return ActionAuditTestConfig.ACTION_ID;
    }

    @Bean
    public Clock getClock() {
      return new FakeClock(ActionAuditTestConfig.INSTANT);
    }
  }

  @Autowired UserServiceAuditor userServiceAuditor;
  @Autowired ActionAuditService mockActionAuditService;

  @After
  public void tearDown() {
    USER = null;
  }

  private static DbUser createUser() {
    final DbUser user = new DbUser();
    user.setUserId(3L);
    user.setUsername("foo@gmail.com");
    return user;
  }

  @Test
  public void testFireUpdateDataAccessAction_userAgent() {
    USER = createUser();
    userServiceAuditor.fireUpdateDataAccessAction(
        USER, DataAccessLevel.UNREGISTERED, DataAccessLevel.REGISTERED, AgentType.USER);
    verify(mockActionAuditService)
        .send(
            argThat(
                (ActionAuditEvent a) ->
                    a.getAgentId() == USER.getUserId()
                        && USER.getUsername().equals(a.getAgentEmailMaybe())
                        && a.getAgentType() == AgentType.USER));
  }

  @Test
  public void testFireUpdateDataAccessAction_systemAgent() {
    userServiceAuditor.fireUpdateDataAccessAction(
        createUser(), DataAccessLevel.UNREGISTERED, DataAccessLevel.REGISTERED, AgentType.SYSTEM);
    verify(mockActionAuditService)
        .send(
            argThat(
                (ActionAuditEvent a) ->
                    a.getAgentId() == 0
                        && a.getAgentEmailMaybe() == null
                        && a.getAgentType() == AgentType.SYSTEM));
  }
}
