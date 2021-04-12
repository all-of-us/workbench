package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.SpringTest;
import org.pmiops.workbench.db.dao.BillingProjectBufferEntryDao.StatusPerTierCount;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbBillingProjectBufferEntry;
import org.pmiops.workbench.db.model.DbBillingProjectBufferEntry.BufferEntryStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class BillingProjectBufferEntryDaoTest extends SpringTest {

  private static final Instant TEST_TIME = Instant.parse("2000-01-01T00:00:00.00Z");

  private static final long ASSIGNED_COUNT = 1;
  private static final long AVAILABLE_COUNT = 2;
  private static final long ERROR_COUNT = 3;
  private static final long CREATING_COUNT = 4;

  private static final ImmutableMap<BufferEntryStatus, Long> STATUS_TO_COUNT_INPUT =
      ImmutableMap.of(
          BufferEntryStatus.ASSIGNED, ASSIGNED_COUNT,
          BufferEntryStatus.AVAILABLE, AVAILABLE_COUNT,
          BufferEntryStatus.ERROR, ERROR_COUNT,
          BufferEntryStatus.CREATING, CREATING_COUNT);

  private static DbAccessTier testAccessTier =
      new DbAccessTier()
          .setAccessTierId(1)
          .setShortName("michael")
          .setAuthDomainGroupEmail("michael@test.org")
          .setAuthDomainName("test.org")
          .setDisplayName("Fancy Display Name")
          .setServicePerimeter("serviceperimeter");

  @Autowired private BillingProjectBufferEntryDao billingProjectBufferEntryDao;
  @Autowired private AccessTierDao accessTierDao;

  @Before
  public void setup() {
    testAccessTier = accessTierDao.save(testAccessTier);
    insertBufferEntriesWithCounts(STATUS_TO_COUNT_INPUT);
  }

  //  @Test
  //  public void testGetCountByStatusMap() {
  //    final Map<BufferEntryStatus, Long> result =
  // billingProjectBufferEntryDao.getCountByStatusMap();
  //    assertThat(result).hasSize(STATUS_TO_COUNT_INPUT.size());
  //    assertThat(result.get(BufferEntryStatus.ASSIGNED)).isEqualTo(ASSIGNED_COUNT);
  //    assertThat(result.get(BufferEntryStatus.CREATING)).isEqualTo(CREATING_COUNT);
  //    assertThat(result.get(BufferEntryStatus.AVAILABLE)).isEqualTo(AVAILABLE_COUNT);
  //    assertThat(result.get(BufferEntryStatus.ERROR)).isEqualTo(ERROR_COUNT);
  //    assertThat(result.get(BufferEntryStatus.GARBAGE_COLLECTED)).isNull();
  //    assertThat(result.get(BufferEntryStatus.ASSIGNING)).isNull();
  //  }

  @Test
  public void testComputeProjectCountByStatus() {
    final List<StatusPerTierCount> rows =
        billingProjectBufferEntryDao.computeProjectCountByStatus();
    assertThat(rows).hasSize(STATUS_TO_COUNT_INPUT.size());
    assertThat(
            rows.stream()
                .map(StatusPerTierCount::getStatusEnum)
                .collect(ImmutableSet.toImmutableSet()))
        .hasSize(STATUS_TO_COUNT_INPUT.size());

    assertThat(
            rows.stream()
                .filter(r -> r.getStatusEnum().equals(BufferEntryStatus.AVAILABLE))
                .map(StatusPerTierCount::getNumProjects)
                .findFirst()
                .orElse(-1L))
        .isEqualTo(AVAILABLE_COUNT);
  }

  @Test
  public void testGetCurrentBufferSizeForAccessTier_invalidTier() {
    DbAccessTier entry = new DbAccessTier();

    assertThat(billingProjectBufferEntryDao.getCurrentBufferSizeForAccessTier(entry)).isEqualTo(0);
  }

  @Test
  public void testGetCurrentBufferSizeForAccessTier_defaultTier() {
    assertThat(billingProjectBufferEntryDao.getCurrentBufferSizeForAccessTier(testAccessTier))
        .isEqualTo(
            STATUS_TO_COUNT_INPUT.get(BufferEntryStatus.AVAILABLE)
                + STATUS_TO_COUNT_INPUT.get(BufferEntryStatus.CREATING));
  }

  @Test
  public void testGetCurrentBufferSizeForAccessTier_multiTier() {
    // add one available project in an alternate tier
    DbAccessTier anotherAccessTier =
        new DbAccessTier()
            .setAccessTierId(2)
            .setShortName("pete")
            .setAuthDomainGroupEmail("pete@test.org")
            .setAuthDomainName("anothertest.org")
            .setDisplayName("Another Fancy Display Name")
            .setServicePerimeter("another-serviceperimeter");

    anotherAccessTier = accessTierDao.save(anotherAccessTier);
    insertBufferEntry(BufferEntryStatus.AVAILABLE, TEST_TIME, anotherAccessTier);

    // no change to existing default tier
    assertThat(billingProjectBufferEntryDao.getCurrentBufferSizeForAccessTier(testAccessTier))
        .isEqualTo(
            STATUS_TO_COUNT_INPUT.get(BufferEntryStatus.AVAILABLE)
                + STATUS_TO_COUNT_INPUT.get(BufferEntryStatus.CREATING));

    // Why is the buffer size 1? - There is an available entry in the correct tier
    assertThat(billingProjectBufferEntryDao.getCurrentBufferSizeForAccessTier(anotherAccessTier))
        .isEqualTo(1);
  }

  @Test
  public void testGetCreatingEntriesToSync() {
    billingProjectBufferEntryDao.deleteAll();

    insertBufferEntry(BufferEntryStatus.ASSIGNED, TEST_TIME, testAccessTier);
    insertBufferEntry(BufferEntryStatus.CREATING, TEST_TIME.plusSeconds(10), testAccessTier);
    insertBufferEntry(BufferEntryStatus.ERROR, TEST_TIME.plusSeconds(20), testAccessTier);
    insertBufferEntry(
        BufferEntryStatus.GARBAGE_COLLECTED, TEST_TIME.plusSeconds(30), testAccessTier);
    insertBufferEntry(BufferEntryStatus.CREATING, TEST_TIME.plusSeconds(40), testAccessTier);
    insertBufferEntry(BufferEntryStatus.CREATING, TEST_TIME.plusSeconds(50), testAccessTier);
    insertBufferEntry(BufferEntryStatus.CREATING, TEST_TIME.plusSeconds(60), testAccessTier);
    insertBufferEntry(BufferEntryStatus.AVAILABLE, TEST_TIME.plusSeconds(70), testAccessTier);

    // confirm that the query retrieves the 3 CREATING entries with the oldest
    // last_sync_request_time time:
    // TEST_TIME.plusSeconds(10), TEST_TIME.plusSeconds(40), and TEST_TIME.plusSeconds(50)
    List<Timestamp> expectedTimestamps =
        Lists.newArrayList(
            Timestamp.from(TEST_TIME.plusSeconds(10)),
            Timestamp.from(TEST_TIME.plusSeconds(40)),
            Timestamp.from(TEST_TIME.plusSeconds(50)));

    List<Timestamp> queriedTimestamps =
        billingProjectBufferEntryDao.getCreatingEntriesToSync(3).stream()
            .map(DbBillingProjectBufferEntry::getLastSyncRequestTime)
            .collect(Collectors.toList());

    assertThat(queriedTimestamps).containsExactlyElementsIn(expectedTimestamps);
  }

  private void insertBufferEntriesWithCounts(Map<BufferEntryStatus, Long> statusToCount) {
    statusToCount.forEach(
        (status, count) -> {
          for (int i = 0; i < count; ++i) {
            insertBufferEntry(status, TEST_TIME, testAccessTier);
          }
        });
  }

  private void insertBufferEntry(
      BufferEntryStatus status, Instant lastUpdatedTime, DbAccessTier accessTier) {
    final DbBillingProjectBufferEntry entry = new DbBillingProjectBufferEntry();
    entry.setAccessTier(accessTier);
    entry.setStatusEnum(status, () -> Timestamp.from(lastUpdatedTime));
    entry.setLastSyncRequestTime(Timestamp.from(lastUpdatedTime));
    billingProjectBufferEntryDao.save(entry);
  }
}
