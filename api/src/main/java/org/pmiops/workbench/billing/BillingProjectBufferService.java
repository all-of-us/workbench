package org.pmiops.workbench.billing;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.hash.Hashing;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.BillingProjectBufferEntryDao;
import org.pmiops.workbench.db.model.DbBillingProjectBufferEntry;
import org.pmiops.workbench.db.model.DbBillingProjectBufferEntry.BufferEntryStatus;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.BillingProjectStatus.CreationStatusEnum;
import org.pmiops.workbench.model.BillingProjectBufferStatus;
import org.pmiops.workbench.utils.Comparables;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BillingProjectBufferService {

  private static final Logger log = Logger.getLogger(BillingProjectBufferService.class.getName());

  private static final int PROJECT_BILLING_ID_SIZE = 8;
  @VisibleForTesting static final Duration CREATING_TIMEOUT = Duration.ofMinutes(60);
  @VisibleForTesting static final Duration ASSIGNING_TIMEOUT = Duration.ofMinutes(10);
  private static ImmutableMap<BufferEntryStatus, Duration> statusToGracePeriod =
      ImmutableMap.of(
          BufferEntryStatus.CREATING, CREATING_TIMEOUT,
          BufferEntryStatus.ASSIGNING, ASSIGNING_TIMEOUT);
  private final BillingProjectBufferEntryDao billingProjectBufferEntryDao;
  private final Clock clock;
  private final FireCloudService fireCloudService;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  @Autowired
  public BillingProjectBufferService(
      BillingProjectBufferEntryDao billingProjectBufferEntryDao,
      Clock clock,
      FireCloudService fireCloudService,
      Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.billingProjectBufferEntryDao = billingProjectBufferEntryDao;
    this.clock = clock;
    this.fireCloudService = fireCloudService;
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  private Timestamp getCurrentTimestamp() {
    return new Timestamp(clock.instant().toEpochMilli());
  }

  /** Makes a configurable number of project creation attempts. */
  public void bufferBillingProjects() {
    int creationAttempts = this.workbenchConfigProvider.get().billing.bufferRefillProjectsPerTask;
    for (int i = 0; i < creationAttempts; i++) {
      bufferBillingProject();
    }
  }

  /**
   * Creates a new billing project in the buffer, and kicks off the FireCloud project creation.
   *
   * <p>No action is taken if the buffer is full.
   */
  private void bufferBillingProject() {
    if (getUnfilledBufferSpace() <= 0) {
      log.info(
          String.format(
              "Billing buffer is at capacity: size = %d, capacity = %d",
              getCurrentBufferSize(), getBufferMaxCapacity()));
      return;
    }

    final String projectName = createBillingProjectName();

    DbBillingProjectBufferEntry entry = new DbBillingProjectBufferEntry();
    entry.setFireCloudProjectName(projectName);
    entry.setCreationTime(new Timestamp(clock.instant().toEpochMilli()));
    entry.setStatusEnum(BufferEntryStatus.CREATING, this::getCurrentTimestamp);
    entry = billingProjectBufferEntryDao.save(entry);

    fireCloudService.createAllOfUsBillingProject(projectName);
    log.info(String.format("Created new project %s", entry.toString()));
  }

  public void syncBillingProjectStatus() {
    List<DbBillingProjectBufferEntry> creatingEntriesToSync =
        billingProjectBufferEntryDao.findTop5ByStatusOrderByLastSyncRequestTimeAsc(
            DbStorageEnums.billingProjectBufferEntryStatusToStorage(BufferEntryStatus.CREATING));
    int successfulSyncCount = 0;
    for (DbBillingProjectBufferEntry entry : creatingEntriesToSync) {

      entry.setLastSyncRequestTime(Timestamp.from(clock.instant()));

      try {
        final CreationStatusEnum fireCloudStatus =
            fireCloudService
                .getBillingProjectStatus(entry.getFireCloudProjectName())
                .getCreationStatus();
        switch (fireCloudStatus) {
          case READY:
            entry.setStatusEnum(BufferEntryStatus.AVAILABLE, this::getCurrentTimestamp);
            log.info(
                String.format(
                    "SyncBillingProjectStatus: BillingProject %s available",
                    entry.getFireCloudProjectName()));
            break;
          case ERROR:
            log.warning(
                String.format(
                    "SyncBillingProjectStatus: BillingProject %s creation failed",
                    entry.getFireCloudProjectName()));
            entry.setStatusEnum(BufferEntryStatus.ERROR, this::getCurrentTimestamp);
            break;
          case CREATING:
          case ADDINGTOPERIMETER:
          default:
            log.info(
                String.format(
                    "SyncBillingProjectStatus: BillingProject %s status=%s",
                    entry.getFireCloudProjectName(), fireCloudStatus.toString()));
            break;
        }
      } catch (NotFoundException e) {
        log.log(
            Level.WARNING,
            "Get BillingProjectStatus call failed for " + entry.toString() + ". Project not found.",
            e);
      } catch (WorkbenchException e) {
        log.log(Level.WARNING, "Get BillingProjectStatus call failed for " + entry.toString(), e);
      }

      billingProjectBufferEntryDao.save(entry);
      successfulSyncCount += 1;
    }
    log.info(
        String.format(
            "Synchronized %d billing projects of %d attempted",
            successfulSyncCount, creatingEntriesToSync.size()));
  }

  /** Update any expired buffer entries in creating or assigning state to ERROR status. */
  public void cleanBillingBuffer() {
    Instant now = clock.instant();
    Iterables.concat(
            findExpiredCreatingEntries(now),
            // For the ASSIGNING status monitor, we can simply filter by the current time as this
            // status tracks internal state rather than the Firecloud status.
            findEntriesWithExpiredGracePeriod(now, BufferEntryStatus.ASSIGNING))
        .forEach(
            entry -> {
              log.warning(
                  "CleanBillingBuffer: Setting status of "
                      + entry.getFireCloudProjectName()
                      + " to ERROR from "
                      + entry.getStatusEnum());
              entry.setStatusEnum(BufferEntryStatus.ERROR, this::getCurrentTimestamp);
              billingProjectBufferEntryDao.save(entry);
            });
  }

  private List<DbBillingProjectBufferEntry> findEntriesWithExpiredGracePeriod(
      Instant now, BufferEntryStatus bufferEntryStatus) {
    final Optional<Duration> gracePeriod =
        Optional.ofNullable(statusToGracePeriod.get(bufferEntryStatus));

    return gracePeriod
        .map(p -> findEntriesWithExpiredGracePeriod(now, bufferEntryStatus, p))
        .orElse(Collections.emptyList());
  }

  private List<DbBillingProjectBufferEntry> findEntriesWithExpiredGracePeriod(
      Instant now, BufferEntryStatus bufferEntryStatus, Duration gracePeriod) {
    return billingProjectBufferEntryDao.findAllByStatusAndLastStatusChangedTimeLessThan(
        DbStorageEnums.billingProjectBufferEntryStatusToStorage(bufferEntryStatus),
        Timestamp.from(now.minus(gracePeriod)));
  }

  private List<DbBillingProjectBufferEntry> findExpiredCreatingEntries(Instant now) {
    return findEntriesWithExpiredGracePeriod(now, BufferEntryStatus.CREATING).stream()
        // Ensure that we've also synchronized the status since the deadline elapsed. This
        // covers degenerate cases where syncBillingProjectStatus might be backlogged and
        // hasn't caught up to check all the CREATING projects, or covers periods where
        // the Firecloud API is unresponsive. Ideally we'd push this logic directly down
        // into the DAO, but JPQL has poor support for date comparisons. Keep the above
        // filter as well as a first pass filter to limit what we pull into memory.
        .filter(entry -> entry.getLastStatusChangedTime() != null)
        .filter(entry -> entry.getLastSyncRequestTime() != null)
        .filter(entry -> Comparables.isLessThan(CREATING_TIMEOUT, entry.getLastChangedToLastSyncRequestInterval()))
        .collect(Collectors.toList());
  }

  private Duration getStatusChangedToLastSync(DbBillingProjectBufferEntry entry) {
    return Duration.between(
        entry.getLastStatusChangedTime().toInstant(), entry.getLastSyncRequestTime().toInstant());
  }

  public DbBillingProjectBufferEntry assignBillingProject(DbUser dbUser) {
    DbBillingProjectBufferEntry entry = consumeBufferEntryForAssignment();

    fireCloudService.addUserToBillingProject(dbUser.getEmail(), entry.getFireCloudProjectName());
    entry.setStatusEnum(BufferEntryStatus.ASSIGNED, this::getCurrentTimestamp);
    entry.setAssignedUser(dbUser);

    // return the updated value
    entry = billingProjectBufferEntryDao.save(entry);
    return entry;
  }

  private DbBillingProjectBufferEntry consumeBufferEntryForAssignment() {
    // Each call to acquire the lock will timeout in 1s if it is currently held
    while (billingProjectBufferEntryDao.acquireAssigningLock() != 1) {}

    DbBillingProjectBufferEntry entry;
    try {
      entry =
          billingProjectBufferEntryDao.findFirstByStatusOrderByCreationTimeAsc(
              DbStorageEnums.billingProjectBufferEntryStatusToStorage(BufferEntryStatus.AVAILABLE));

      if (entry == null) {
        log.log(Level.SEVERE, "Consume Buffer call made while Billing Project Buffer was empty");
        throw new EmptyBufferException();
      }

      entry.setStatusEnum(BufferEntryStatus.ASSIGNING, this::getCurrentTimestamp);
      billingProjectBufferEntryDao.save(entry);
    } finally {
      billingProjectBufferEntryDao.releaseAssigningLock();
    }

    return entry;
  }

  private String createBillingProjectName() {
    String randomString =
        Hashing.sha256()
            .hashUnencodedChars(UUID.randomUUID().toString())
            .toString()
            .substring(0, PROJECT_BILLING_ID_SIZE);

    String prefix = workbenchConfigProvider.get().billing.projectNamePrefix;
    if (!prefix.endsWith("-")) {
      prefix = prefix + "-";
    }

    return prefix + randomString;
  }

  private int getUnfilledBufferSpace() {
    return getBufferMaxCapacity() - (int) getCurrentBufferSize();
  }

  private long getCurrentBufferSize() {
    return billingProjectBufferEntryDao.getCurrentBufferSize();
  }

  private int getBufferMaxCapacity() {
    return workbenchConfigProvider.get().billing.bufferCapacity;
  }

  public BillingProjectBufferStatus getStatus() {
    final long bufferSize =
        billingProjectBufferEntryDao.countByStatus(
            DbStorageEnums.billingProjectBufferEntryStatusToStorage(BufferEntryStatus.AVAILABLE));
    return new BillingProjectBufferStatus().bufferSize(bufferSize);
  }
}
