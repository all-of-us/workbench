package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.config.CommonConfig;
import org.pmiops.workbench.db.model.DbEgressEvent;
import org.pmiops.workbench.db.model.DbEgressEvent.DbEgressEventStatus;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@Import({FakeClockConfiguration.class, CommonConfig.class})
@DataJpaTest
public class EgressEventDaoTest {
  @Autowired private UserDao userDao;

  @Autowired private EgressEventDao egressEventDao;

  private static final Timestamp CURRENT_TIMESTAMP = Timestamp.from(Instant.now());
  private DbUser user;

  @BeforeEach
  public void setUp() {
    user = userDao.save(new DbUser());
  }

  @Test
  public void testCreateAndGet() {
    // Create
    DbEgressEvent event = createValidDbEgressEvent();
    event = egressEventDao.save(event);
    assertThat(event.getEgressEventId()).isNotNull();

    // Read
    DbEgressEvent foundEvent = egressEventDao.findById(event.getEgressEventId()).orElse(null);
    assertThat(foundEvent).isNotNull();
    assertThat(foundEvent.getUser()).isEqualTo(user);
    assertThat(foundEvent.getVwbWorkspaceId()).isEqualTo("testWorkspaceId");
    assertThat(foundEvent.getVwbVmName()).isEqualTo("testVmName");
    assertThat(foundEvent.getGcpProjectId()).isEqualTo("test-project");
    assertThat(foundEvent.getIsVwb()).isTrue();

    // Update
    foundEvent.setStatus(DbEgressEventStatus.VERIFIED_FALSE_POSITIVE);
    egressEventDao.save(foundEvent);
    DbEgressEvent updatedEvent = egressEventDao.findById(event.getEgressEventId()).orElse(null);
    assertThat(updatedEvent.getStatus())
        .isEqualTo(DbEgressEvent.DbEgressEventStatus.VERIFIED_FALSE_POSITIVE);
  }

  @Test
  public void testFindAllByUserAndStatusNotIn() {
    DbEgressEvent event1 = createValidDbEgressEvent();
    event1.setStatus(DbEgressEventStatus.PENDING);
    egressEventDao.save(event1);

    DbEgressEvent event2 = createValidDbEgressEvent();
    event2.setStatus(DbEgressEventStatus.REMEDIATED);
    egressEventDao.save(event2);

    DbEgressEvent event3 = createValidDbEgressEvent();
    event3.setStatus(DbEgressEventStatus.VERIFIED_FALSE_POSITIVE);
    egressEventDao.save(event3);

    List<DbEgressEventStatus> excludedStatuses =
        List.of(DbEgressEventStatus.REMEDIATED, DbEgressEventStatus.VERIFIED_FALSE_POSITIVE);
    List<DbEgressEvent> result = egressEventDao.findAllByUserAndStatusNotIn(user, excludedStatuses);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getStatus()).isEqualTo(DbEgressEventStatus.PENDING);
  }

  private DbEgressEvent createValidDbEgressEvent() {
    DbEgressEvent event = new DbEgressEvent();
    event.setUser(user);
    event.setWorkspace(new DbWorkspace());
    event.setCreationTime(CURRENT_TIMESTAMP);
    event.setStatus(DbEgressEvent.DbEgressEventStatus.PENDING);
    event.setEgressWindowSeconds(3600L);
    event.setVwbWorkspaceId("testWorkspaceId");
    event.setVwbVmName("testVmName");
    event.setGcpProjectId("test-project");
    event.setIsVwb(true);
    return event;
  }
}
