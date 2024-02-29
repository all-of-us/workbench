package org.pmiops.workbench.actionaudit.auditors;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;
import static org.pmiops.workbench.utils.TestMockFactory.createControlledTier;
import static org.pmiops.workbench.utils.TestMockFactory.createRegisteredTier;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.pmiops.workbench.FakeClockConfiguration;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

@DataJpaTest
public class UserServiceAuditorTest {
  @TestConfiguration
  @Import({FakeClockConfiguration.class, UserServiceAuditorImpl.class, ActionAuditTestConfig.class})
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
    DbAccessTier registeredTier = accessTierDao.save(createRegisteredTier());
    List<DbAccessTier> newTiers = Collections.singletonList(registeredTier);

    DbUser user = createUser();
    userServiceAuditor.fireUpdateAccessTiersAction(
        user, Collections.emptyList(), newTiers, Agent.asUser(user));
    verify(mockActionAuditService).send(eventArg.capture());

    ActionAuditEvent event = eventArg.getValue();
    assertThat(event.agentType()).isEqualTo(AgentType.USER);
    assertThat(event.agentIdMaybe()).isEqualTo(user.getUserId());
    assertThat(event.agentEmailMaybe()).isEqualTo(user.getUsername());
    assertThat(event.previousValueMaybe()).isEmpty();
    assertThat(event.newValueMaybe()).isEqualTo(registeredTier.getShortName());
  }

  @Test
  public void testFireUpdateAccessTiersAction_userAgent_unregister() {
    DbAccessTier registeredTier = accessTierDao.save(createRegisteredTier());
    List<DbAccessTier> oldTiers = Collections.singletonList(registeredTier);

    DbUser user = createUser();
    userServiceAuditor.fireUpdateAccessTiersAction(
        user, oldTiers, Collections.emptyList(), Agent.asUser(user));
    verify(mockActionAuditService).send(eventArg.capture());

    ActionAuditEvent event = eventArg.getValue();
    assertThat(event.agentType()).isEqualTo(AgentType.USER);
    assertThat(event.agentIdMaybe()).isEqualTo(user.getUserId());
    assertThat(event.agentEmailMaybe()).isEqualTo(user.getUsername());
    assertThat(event.previousValueMaybe()).isEqualTo(registeredTier.getShortName());
    assertThat(event.newValueMaybe()).isEmpty();
  }

  @Test
  public void testFireUpdateAccessTiersAction_systemAgent_addControlled() {
    DbAccessTier registeredTier = accessTierDao.save(createRegisteredTier());
    DbAccessTier controlledTier = accessTierDao.save(createControlledTier());
    List<DbAccessTier> oldTiers = Collections.singletonList(registeredTier);
    List<DbAccessTier> newTiers = ImmutableList.of(registeredTier, controlledTier);

    userServiceAuditor.fireUpdateAccessTiersAction(
        createUser(), oldTiers, newTiers, Agent.asSystem());
    verify(mockActionAuditService).send(eventArg.capture());

    ActionAuditEvent event = eventArg.getValue();
    assertThat(event.agentType()).isEqualTo(AgentType.SYSTEM);
    assertThat(event.agentIdMaybe()).isNull();
    assertThat(event.agentEmailMaybe()).isNull();
    assertThat(event.previousValueMaybe()).isEqualTo(registeredTier.getShortName());
    List<String> expected =
        ImmutableList.of(registeredTier.getShortName(), controlledTier.getShortName());
    assertThat(split(event.newValueMaybe())).containsExactlyElementsIn(expected);
  }

  private Iterable<String> split(String input) {
    assertThat(input).isNotNull();
    return Splitter.on(',').split(input);
  }

  @Test
  public void testSetFreeTierDollarQuota_initial() {
    DbUser user = createUser();
    userServiceAuditor.fireSetFreeTierDollarLimitOverride(user.getUserId(), null, 123.45);
    verify(mockActionAuditService).send(eventArg.capture());

    ActionAuditEvent eventSent = eventArg.getValue();
    assertThat(eventSent.actionType()).isEqualTo(ActionType.EDIT);
    assertThat(eventSent.agentType()).isEqualTo(AgentType.ADMINISTRATOR);
    assertThat(eventSent.targetType()).isEqualTo(TargetType.ACCOUNT);
    assertThat(eventSent.targetIdMaybe()).isEqualTo(user.getUserId());
    assertThat(eventSent.targetPropertyMaybe())
        .isEqualTo(AccountTargetProperty.FREE_TIER_DOLLAR_QUOTA.getPropertyName());
    assertThat(eventSent.previousValueMaybe()).isNull();
    assertThat(eventSent.newValueMaybe()).isEqualTo("123.45");
  }

  @Test
  public void testSetFreeTierDollarQuota_chnge() {
    DbUser user = createUser();
    userServiceAuditor.fireSetFreeTierDollarLimitOverride(user.getUserId(), 123.45, 500.0);
    verify(mockActionAuditService).send(eventArg.capture());

    ActionAuditEvent eventSent = eventArg.getValue();
    assertThat(eventSent.actionType()).isEqualTo(ActionType.EDIT);
    assertThat(eventSent.agentType()).isEqualTo(AgentType.ADMINISTRATOR);
    assertThat(eventSent.targetType()).isEqualTo(TargetType.ACCOUNT);
    assertThat(eventSent.targetIdMaybe()).isEqualTo(user.getUserId());
    assertThat(eventSent.targetPropertyMaybe())
        .isEqualTo(AccountTargetProperty.FREE_TIER_DOLLAR_QUOTA.getPropertyName());
    assertThat(eventSent.previousValueMaybe()).isEqualTo("123.45");
    assertThat(eventSent.newValueMaybe()).isEqualTo("500.0");
  }
}
