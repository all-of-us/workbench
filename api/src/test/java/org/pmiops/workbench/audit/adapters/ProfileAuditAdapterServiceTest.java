package org.pmiops.workbench.audit.adapters;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
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
import org.pmiops.workbench.audit.AgentType;
import org.pmiops.workbench.audit.TargetType;
import org.pmiops.workbench.db.model.User;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class ProfileAuditAdapterServiceTest {

  private static final long USER_ID = 101L;
  private static final String USER_EMAIL = "a@b.com";
  private static final long Y2K_EPOCH_MILLIS =
      Instant.parse("2000-01-01T00:00:00.00Z").toEpochMilli();

  @Mock private Provider<User> mockUserProvider;
  @Mock private ActionAuditService mockActionAuditService;
  @Mock private Clock mockClock;

  @Captor private ArgumentCaptor<Collection<ActionAuditEvent>> eventListCaptor;
  @Captor private ArgumentCaptor<ActionAuditEvent> eventCaptor;
  private ProfileAuditAdapterService profileAuditAdapterService;

  @Before
  public void setUp() {
    profileAuditAdapterService =
        new ProfileAuditAdapterServiceImpl(mockUserProvider, mockActionAuditService, mockClock);
    doReturn(Y2K_EPOCH_MILLIS).when(mockClock).millis();
  }

  @Test
  public void testDeleteUserProfile() {
    profileAuditAdapterService.fireDeleteAction(USER_ID, USER_EMAIL);
    verify(mockActionAuditService).send(eventCaptor.capture());
    ActionAuditEvent eventSent = eventCaptor.getValue();

    assertThat(eventSent.targetType()).isEqualTo(TargetType.PROFILE);
    assertThat(eventSent.targetId().isPresent()).isTrue();
    assertThat(eventSent.agentType()).isEqualTo(AgentType.USER);
    assertThat(eventSent.agentId()).isEqualTo(USER_ID);
    assertThat(eventSent.targetId().get()).isEqualTo(USER_ID);
    assertThat(eventSent.actionType()).isEqualTo(ActionType.DELETE);
    assertThat(eventSent.timestamp()).isEqualTo(Y2K_EPOCH_MILLIS);
    assertThat(eventSent.targetProperty().isPresent()).isFalse();
    assertThat(eventSent.newValue().isPresent()).isFalse();
    assertThat(eventSent.previousValue().isPresent()).isFalse();
  }
}
