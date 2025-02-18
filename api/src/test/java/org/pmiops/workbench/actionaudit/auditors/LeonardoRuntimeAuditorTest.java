package org.pmiops.workbench.actionaudit.auditors;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import jakarta.inject.Provider;
import java.time.Clock;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.actionaudit.ActionAuditEvent;
import org.pmiops.workbench.actionaudit.ActionAuditService;
import org.pmiops.workbench.actionaudit.ActionType;
import org.pmiops.workbench.db.model.DbUser;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
public class LeonardoRuntimeAuditorTest {
  private DbUser user1;
  private static final long Y2K_EPOCH_MILLIS =
      Instant.parse("2000-01-01T00:00:00.00Z").toEpochMilli();
  private static final String ACTION_ID = "58cbae08-447f-499f-95b9-7bdedc955f4d";

  private LeonardoRuntimeAuditor leonardoRuntimeauditor;

  @Captor private ArgumentCaptor<ActionAuditEvent> eventCollectionCaptor;

  @Mock private Provider<String> mockActionIdProvider;
  @Mock private ActionAuditService mockActionAuditService;
  @Mock private Clock mockClock;
  @Mock private Provider<DbUser> mockUserProvider;

  @TestConfiguration
  @Import(FakeClockConfiguration.class)
  @MockBean(value = {ActionAuditService.class})
  static class Configuration {}

  @BeforeEach
  public void setUp() {
    user1 = new DbUser();
    user1.setUserId(101L);
    user1.setUsername("fflinstone@slate.com");
    user1.setGivenName("Fred");
    user1.setFamilyName("Flintstone");
    doReturn(user1).when(mockUserProvider).get();
    leonardoRuntimeauditor =
        new LeonardoRuntimeAuditorImpl(
            mockActionIdProvider, mockActionAuditService, mockClock, mockUserProvider);

    doReturn(Y2K_EPOCH_MILLIS).when(mockClock).millis();
    doReturn(ACTION_ID).when(mockActionIdProvider).get();
  }

  @Test
  public void testFireDeleteRuntime() {
    String runtimeName = "my-runtime";
    String googleProject = "my-project";
    leonardoRuntimeauditor.fireDeleteRuntime(googleProject, runtimeName);
    verify(mockActionAuditService).send(eventCollectionCaptor.capture());
    ActionAuditEvent eventSent = eventCollectionCaptor.getValue();
    assertThat(eventSent).isNotNull();
    assertThat(eventSent.actionType()).isEqualTo(ActionType.DELETE);
    assertThat(eventSent.targetPropertyMaybe()).isEqualTo(googleProject);
    assertThat(eventSent.newValueMaybe()).isEqualTo(runtimeName);
  }
}
