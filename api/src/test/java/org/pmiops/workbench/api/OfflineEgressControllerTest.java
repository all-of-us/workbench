package org.pmiops.workbench.api;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.FakeJpaDateTimeConfiguration;
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.db.dao.EgressEventDao;
import org.pmiops.workbench.db.model.DbEgressEvent;
import org.pmiops.workbench.db.model.DbEgressEvent.DbEgressEventStatus;
import org.pmiops.workbench.test.FakeClock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(FakeJpaDateTimeConfiguration.class)
public class OfflineEgressControllerTest {
  private static final Instant TEN_MINUTES_AGO =
      FakeClockConfiguration.NOW.toInstant().minus(Duration.ofMinutes(10));
  private static final Instant TWO_HOURS_AGO =
      FakeClockConfiguration.NOW.toInstant().minus(Duration.ofHours(2L));

  @Autowired private FakeClock fakeClock;
  @MockBean private TaskQueueService mockTaskQueueService;
  @Autowired private EgressEventDao egressEventDao;
  @Autowired private OfflineEgressController offlineEgressController;

  @Import({FakeClockConfiguration.class, OfflineEgressController.class})
  @TestConfiguration
  static class Configuration {}

  @AfterEach
  public void tearDown() {
    egressEventDao.deleteAll();
  }

  @Test
  public void testCheckPendingEgressEvents() {
    fakeClock.setInstant(TWO_HOURS_AGO);
    egressEventDao.save(newEvent());
    egressEventDao.save(newEvent());

    fakeClock.setInstant(TEN_MINUTES_AGO);
    egressEventDao.save(newEvent());

    fakeClock.setInstant(FakeClockConfiguration.NOW.toInstant());
    offlineEgressController.checkPendingEgressEvents();

    verify(mockTaskQueueService, times(2)).pushEgressEventTask(anyLong());
  }

  @Test
  public void testCheckPendingEgressEvents_noMatches() {
    fakeClock.setInstant(TWO_HOURS_AGO);
    egressEventDao.save(newEvent().setStatus(DbEgressEventStatus.REMEDIATED));

    fakeClock.setInstant(TEN_MINUTES_AGO);
    egressEventDao.save(newEvent());

    fakeClock.setInstant(FakeClockConfiguration.NOW.toInstant());
    offlineEgressController.checkPendingEgressEvents();

    verifyNoInteractions(mockTaskQueueService);
  }

  private DbEgressEvent newEvent() {
    return new DbEgressEvent().setStatus(DbEgressEventStatus.PENDING);
  }
}
