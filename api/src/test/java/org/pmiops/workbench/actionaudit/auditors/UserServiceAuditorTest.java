package org.pmiops.workbench.actionaudit.auditors;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.pmiops.workbench.actionaudit.ActionAuditEvent;
import org.pmiops.workbench.actionaudit.ActionAuditService;
import org.pmiops.workbench.actionaudit.ActionType;
import org.pmiops.workbench.actionaudit.Agent;
import org.pmiops.workbench.actionaudit.AgentType;
import org.pmiops.workbench.actionaudit.TargetType;
import org.pmiops.workbench.actionaudit.targetproperties.AccountTargetProperty;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.utils.TestMockFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
public class UserServiceAuditorTest {
  @TestConfiguration
  @Import({UserServiceAuditorImpl.class, ActionAuditTestConfig.class})
  @MockBean(ActionAuditService.class)
  static class Configuration {}

  @Captor ArgumentCaptor<ActionAuditEvent> eventArg;

  @Autowired UserServiceAuditor userServiceAuditor;
  @Autowired ActionAuditService mockActionAuditService;

  @Autowired AccessTierDao accessTierDao;

  private static DbUser createUser() {
    final DbUser user = new DbUser();
    user.setUserId(3L);
    user.setUsername("foo@gmail.com");
    return user;
  }

  @Test
  public void testFireUpdateAccessTiersAction_userAgent_register() {
    DbAccessTier registeredTier = TestMockFactory.createRegisteredTierForTests(accessTierDao);
    List<DbAccessTier> newTiers = Collections.singletonList(registeredTier);

    DbUser user = createUser();
    userServiceAuditor.fireUpdateAccessTiersAction(
        user, Collections.emptyList(), newTiers, Agent.asUser(user));
    verify(mockActionAuditService).send(eventArg.capture());

    ActionAuditEvent event = eventArg.getValue();
    assertThat(event.getAgentType()).isEqualTo(AgentType.USER);
    assertThat(event.getAgentIdMaybe()).isEqualTo(user.getUserId());
    assertThat(event.getAgentEmailMaybe()).isEqualTo(user.getUsername());
    assertThat(event.getPreviousValueMaybe()).isEmpty();
    assertThat(event.getNewValueMaybe()).isEqualTo(registeredTier.getShortName());
  }

  @Test
  public void testFireUpdateAccessTiersAction_userAgent_unregister() {
    DbAccessTier registeredTier = TestMockFactory.createRegisteredTierForTests(accessTierDao);
    List<DbAccessTier> oldTiers = Collections.singletonList(registeredTier);

    DbUser user = createUser();
    userServiceAuditor.fireUpdateAccessTiersAction(
        user, oldTiers, Collections.emptyList(), Agent.asUser(user));
    verify(mockActionAuditService).send(eventArg.capture());

    ActionAuditEvent event = eventArg.getValue();
    assertThat(event.getAgentType()).isEqualTo(AgentType.USER);
    assertThat(event.getAgentIdMaybe()).isEqualTo(user.getUserId());
    assertThat(event.getAgentEmailMaybe()).isEqualTo(user.getUsername());
    assertThat(event.getPreviousValueMaybe()).isEqualTo(registeredTier.getShortName());
    assertThat(event.getNewValueMaybe()).isEmpty();
  }

  @Test
  public void testFireUpdateAccessTiersAction_systemAgent_addControlled() {
    DbAccessTier registeredTier = TestMockFactory.createRegisteredTierForTests(accessTierDao);
    DbAccessTier controlledTier = TestMockFactory.createControlledTierForTests(accessTierDao);
    List<DbAccessTier> oldTiers = Collections.singletonList(registeredTier);
    List<DbAccessTier> newTiers = ImmutableList.of(registeredTier, controlledTier);

    userServiceAuditor.fireUpdateAccessTiersAction(
        createUser(), oldTiers, newTiers, Agent.asSystem());
    verify(mockActionAuditService).send(eventArg.capture());

    ActionAuditEvent event = eventArg.getValue();
    assertThat(event.getAgentType()).isEqualTo(AgentType.SYSTEM);
    assertThat(event.getAgentIdMaybe()).isNull();
    assertThat(event.getAgentEmailMaybe()).isNull();
    assertThat(event.getPreviousValueMaybe()).isEqualTo(registeredTier.getShortName());
    String expected =
        String.format("%s,%s", registeredTier.getShortName(), controlledTier.getShortName());
    assertThat(event.getNewValueMaybe()).isEqualTo(expected);
  }

  @Test
  public void testSetFreeTierDollarQuota_initial() {
    DbUser user = createUser();
    userServiceAuditor.fireSetFreeTierDollarLimitOverride(user.getUserId(), null, 123.45);
    verify(mockActionAuditService).send(eventArg.capture());

    ActionAuditEvent eventSent = eventArg.getValue();
    assertThat(eventSent.getActionType()).isEqualTo(ActionType.EDIT);
    assertThat(eventSent.getAgentType()).isEqualTo(AgentType.ADMINISTRATOR);
    assertThat(eventSent.getTargetType()).isEqualTo(TargetType.ACCOUNT);
    assertThat(eventSent.getTargetIdMaybe()).isEqualTo(user.getUserId());
    assertThat(eventSent.getTargetPropertyMaybe())
        .isEqualTo(AccountTargetProperty.FREE_TIER_DOLLAR_QUOTA.getPropertyName());
    assertThat(eventSent.getPreviousValueMaybe()).isNull();
    assertThat(eventSent.getNewValueMaybe()).isEqualTo("123.45");
  }

  @Test
  public void testSetFreeTierDollarQuota_chnge() {
    DbUser user = createUser();
    userServiceAuditor.fireSetFreeTierDollarLimitOverride(user.getUserId(), 123.45, 500.0);
    verify(mockActionAuditService).send(eventArg.capture());

    ActionAuditEvent eventSent = eventArg.getValue();
    assertThat(eventSent.getActionType()).isEqualTo(ActionType.EDIT);
    assertThat(eventSent.getAgentType()).isEqualTo(AgentType.ADMINISTRATOR);
    assertThat(eventSent.getTargetType()).isEqualTo(TargetType.ACCOUNT);
    assertThat(eventSent.getTargetIdMaybe()).isEqualTo(user.getUserId());
    assertThat(eventSent.getTargetPropertyMaybe())
        .isEqualTo(AccountTargetProperty.FREE_TIER_DOLLAR_QUOTA.getPropertyName());
    assertThat(eventSent.getPreviousValueMaybe()).isEqualTo("123.45");
    assertThat(eventSent.getNewValueMaybe()).isEqualTo("500.0");
  }
}
