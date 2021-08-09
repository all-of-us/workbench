package org.pmiops.workbench.billing;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.hash.Hashing;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Provider;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.pmiops.workbench.access.AccessTierService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.BillingProjectBufferEntryDao;
import org.pmiops.workbench.db.dao.BillingProjectBufferEntryDao.ProjectCountByStatusAndTier;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbBillingProjectBufferEntry;
import org.pmiops.workbench.db.model.DbBillingProjectBufferEntry.BufferEntryStatus;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudBillingProjectStatus.CreationStatusEnum;
import org.pmiops.workbench.model.AvailableBufferPerTier;
import org.pmiops.workbench.model.BillingProjectBufferStatus;
import org.pmiops.workbench.monitoring.GaugeDataCollector;
import org.pmiops.workbench.monitoring.MeasurementBundle;
import org.pmiops.workbench.monitoring.labels.MetricLabel;
import org.pmiops.workbench.monitoring.views.GaugeMetric;
import org.pmiops.workbench.utils.Comparables;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BillingProjectBufferService implements GaugeDataCollector {

  private static final Logger log = Logger.getLogger(BillingProjectBufferService.class.getName());
  @VisibleForTesting static final Duration CREATING_TIMEOUT = Duration.ofMinutes(60);
  @VisibleForTesting static final Duration ASSIGNING_TIMEOUT = Duration.ofMinutes(10);
  @VisibleForTesting static final int PROJECT_BILLING_ID_SIZE = 8;
  private static final ImmutableMap<BufferEntryStatus, Duration> STATUS_TO_GRACE_PERIOD =
      ImmutableMap.of(
          BufferEntryStatus.CREATING, CREATING_TIMEOUT,
          BufferEntryStatus.ASSIGNING, ASSIGNING_TIMEOUT);

  private final AccessTierService accessTierService;
  private final BillingProjectBufferEntryDao billingProjectBufferEntryDao;
  private final Clock clock;
  private final FireCloudService fireCloudService;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  @Autowired
  public BillingProjectBufferService(
      AccessTierService accessTierService,
      BillingProjectBufferEntryDao billingProjectBufferEntryDao,
      Clock clock,
      FireCloudService fireCloudService,
      Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.accessTierService = accessTierService;
    this.billingProjectBufferEntryDao = billingProjectBufferEntryDao;
    this.clock = clock;
    this.fireCloudService = fireCloudService;
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  private Timestamp getCurrentTimestamp() {
    return Timestamp.from(clock.instant());
  }

  /** Makes a configurable number of project creation attempts per access tier. */
  public void bufferBillingProjects() {
    WorkbenchConfig config = this.workbenchConfigProvider.get();

    for (DbAccessTier accessTier : accessTierService.getAllTiers()) {
      for (int i = 0; i < config.billing.bufferRefillProjectsPerTask; i++) {
        bufferBillingProject(accessTier);
      }
    }
  }

  @Override
  public Collection<MeasurementBundle> getGaugeData() {
    // retrieve those combinations of access tier and buffer status with at least 1 current project
    final Map<Pair<DbAccessTier, BufferEntryStatus>, Long> counts =
        billingProjectBufferEntryDao.getBillingBufferGaugeData().stream()
            .collect(
                Collectors.toMap(
                    c -> Pair.of(c.getAccessTier(), c.getStatusEnum()),
                    ProjectCountByStatusAndTier::getNumProjects));

    // iterate through ALL combinations of access tier and buffer status, counting 0 projects where
    // this combination does not appear in `counts`
    return accessTierService.getAllTiers().stream()
        .flatMap(
            tier ->
                Arrays.stream(BufferEntryStatus.values())
                    .flatMap(
                        status -> {
                          final long numProjects = counts.getOrDefault(Pair.of(tier, status), 0L);
                          return Stream.of(
                              MeasurementBundle.builder()
                                  .addMeasurement(
                                      GaugeMetric.BILLING_BUFFER_PROJECT_COUNT, numProjects)
                                  .addTag(MetricLabel.BUFFER_ENTRY_STATUS, status.toString())
                                  .addTag(MetricLabel.ACCESS_TIER_SHORT_NAME, tier.getShortName())
                                  .build());
                        }))
        .collect(Collectors.toList());
  }

  /**
   * Creates a random Billing Project name. This actually should be in separate service. But
   * consider billing buffer will be gone soon, it is easier to put the method in this class, and
   * just rename the class to BillingProjectService once start deprecation.
   */
  public String createBillingProjectName() {
    String randomString =
        Hashing.sha256()
            .hashUnencodedChars(UUID.randomUUID().toString())
            .toString()
            .substring(0, PROJECT_BILLING_ID_SIZE);

    String projectNamePrefix = workbenchConfigProvider.get().billing.projectNamePrefix;
    if (!projectNamePrefix.endsWith("-")) {
      projectNamePrefix = projectNamePrefix + "-";
    }
    return projectNamePrefix + randomString;
  }

  /**
   * Creates a new billing project in the buffer, and kicks off the FireCloud project creation.
   *
   * <p>No action is taken if the buffer is full.
   */
  private void bufferBillingProject(DbAccessTier accessTier) {
    if (getUnfilledBufferSpace(accessTier) <= 0) {
      log.fine(
          String.format(
              "Billing buffer for tier '%s' is at capacity: size = %d, capacity = %d",
              accessTier.getDisplayName(),
              getCurrentBufferSize(accessTier),
              getBufferMaxCapacity(accessTier.getShortName())));
      return;
    }

    final DbBillingProjectBufferEntry creatingBufferEntry = makeCreatingBufferEntry(accessTier);
    fireCloudService.createAllOfUsBillingProject(
        creatingBufferEntry.getFireCloudProjectName(), accessTier.getServicePerimeter());
    log.info(
        String.format(
            "Created new project %s for access tier '%s'",
            creatingBufferEntry.getFireCloudProjectName(), accessTier.getDisplayName()));
  }

  @NotNull
  private DbBillingProjectBufferEntry makeCreatingBufferEntry(DbAccessTier accessTier) {
    final DbBillingProjectBufferEntry bufferEntry = new DbBillingProjectBufferEntry();
    bufferEntry.setFireCloudProjectName(createBillingProjectName());
    bufferEntry.setCreationTime(Timestamp.from(clock.instant()));
    // Note: we set the lastSyncRequestTime column to the current timestamp as an optimization.
    // If we leave this column as NULL, the sync process will prioritize this entry for immediate
    // synchronization with Terra. Instead, we populate this column with the current timestamp
    // since we know with high confidence that the initial status is CREATING. See RW-6192 for
    // more context.
    bufferEntry.setLastSyncRequestTime(Timestamp.from(clock.instant()));
    bufferEntry.setStatusEnum(BufferEntryStatus.CREATING, this::getCurrentTimestamp);
    bufferEntry.setAccessTier(accessTier);
    return billingProjectBufferEntryDao.save(bufferEntry);
  }

  public void syncBillingProjectStatus() {
    List<DbBillingProjectBufferEntry> creatingEntriesToSync =
        billingProjectBufferEntryDao.getCreatingEntriesToSync(
            workbenchConfigProvider.get().billing.bufferStatusChecksPerTask);
    if (creatingEntriesToSync.isEmpty()) {
      log.info("No entries to sync!");
      return;
    }

    int successfulSyncCount = 0;
    for (DbBillingProjectBufferEntry bufferEntry : creatingEntriesToSync) {
      //noinspection UnusedAssignment
      bufferEntry = syncBufferEntry(bufferEntry);
      successfulSyncCount += 1;
    }
    log.info(
        String.format(
            "Synchronized %d/%d billing projects.",
            successfulSyncCount, creatingEntriesToSync.size()));
  }

  private DbBillingProjectBufferEntry syncBufferEntry(DbBillingProjectBufferEntry bufferEntry) {
    bufferEntry.setLastSyncRequestTime(Timestamp.from(clock.instant()));

    try {
      final CreationStatusEnum fireCloudStatus =
          fireCloudService
              .getBillingProjectStatus(bufferEntry.getFireCloudProjectName())
              .getCreationStatus();
      switch (fireCloudStatus) {
        case READY:
          bufferEntry.setStatusEnum(BufferEntryStatus.AVAILABLE, this::getCurrentTimestamp);
          log.info(
              String.format(
                  "SyncBillingProjectStatus: BillingProject %s available", bufferEntry.toString()));
          break;
        case ERROR:
          log.warning(
              String.format(
                  "SyncBillingProjectStatus: BillingProject %s creation failed",
                  bufferEntry.toString()));
          bufferEntry.setStatusEnum(BufferEntryStatus.ERROR, this::getCurrentTimestamp);
          break;
        case CREATING:
        case ADDINGTOPERIMETER:
        default:
          log.info(
              String.format(
                  "SyncBillingProjectStatus: BillingProject %s Firecloud status = %s",
                  bufferEntry.toString(), fireCloudStatus.toString()));
          break;
      }
    } catch (NotFoundException e) {
      log.log(
          Level.WARNING,
          String.format(
              "Failed to get Firecloud status for %s. Project not found.", bufferEntry.toString()),
          e);
    } catch (WorkbenchException e) {
      log.log(
          Level.WARNING, "Get BillingProjectStatus call failed for " + bufferEntry.toString(), e);
    }
    return billingProjectBufferEntryDao.save(bufferEntry);
  }

  /** Update any expired buffer entries in creating or assigning state to ERROR status. */
  public void cleanBillingBuffer() {
    Instant now = clock.instant();
    Iterables.concat(
            findExpiredCreatingEntries(now),
            // For the ASSIGNING status monitor, we can simply filter by the current time as this
            // status tracks internal state rather than the Firecloud status.
            findEntriesWithExpiredGracePeriod(now, BufferEntryStatus.ASSIGNING))
        .forEach(this::setBufferEntryErrorStatus);
  }

  private void setBufferEntryErrorStatus(DbBillingProjectBufferEntry bufferEntry) {
    log.warning(
        "CleanBillingBuffer: Setting status of "
            + bufferEntry.getFireCloudProjectName()
            + " to ERROR from "
            + bufferEntry.getStatusEnum());
    bufferEntry.setStatusEnum(BufferEntryStatus.ERROR, this::getCurrentTimestamp);
    //noinspection UnusedAssignment
    bufferEntry = billingProjectBufferEntryDao.save(bufferEntry);
  }

  @VisibleForTesting
  List<DbBillingProjectBufferEntry> findEntriesWithExpiredGracePeriod(
      Instant now, BufferEntryStatus bufferEntryStatus) {
    final Optional<Duration> gracePeriod =
        Optional.ofNullable(STATUS_TO_GRACE_PERIOD.get(bufferEntryStatus));

    return gracePeriod
        .map(
            p ->
                billingProjectBufferEntryDao.findOlderThanByStatus(now.minus(p), bufferEntryStatus))
        .orElse(Collections.emptyList());
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
        .filter(
            entry ->
                Comparables.isLessThan(
                    CREATING_TIMEOUT, entry.getLastChangedToLastSyncRequestInterval()))
        .collect(Collectors.toList());
  }

  public DbBillingProjectBufferEntry assignBillingProject(
      DbUser dbUser, DbAccessTier dbAccessTier) {
    DbBillingProjectBufferEntry bufferEntry = consumeBufferEntryForAssignment(dbAccessTier);

    fireCloudService.addOwnerToBillingProject(
        dbUser.getUsername(), bufferEntry.getFireCloudProjectName());
    bufferEntry.setStatusEnum(BufferEntryStatus.ASSIGNED, this::getCurrentTimestamp);
    bufferEntry.setAssignedUser(dbUser);

    // Ensure entry reference isn't left in a dirty state by save().
    bufferEntry = billingProjectBufferEntryDao.save(bufferEntry);
    return bufferEntry;
  }

  private DbBillingProjectBufferEntry consumeBufferEntryForAssignment(DbAccessTier accessTier) {
    // Each call to acquire the lock will timeout in 1s if it is currently held
    while (true) {
      if (billingProjectBufferEntryDao.acquireAssigningLock() == 1) {
        break;
      }
    }

    DbBillingProjectBufferEntry entry;
    try {
      entry =
          billingProjectBufferEntryDao.findOldestForStatus(BufferEntryStatus.AVAILABLE, accessTier);
      if (entry == null) {
        log.log(Level.SEVERE, "Consume Buffer call made while Billing Project Buffer was empty");
        throw new EmptyBufferException();
      }

      entry.setStatusEnum(BufferEntryStatus.ASSIGNING, this::getCurrentTimestamp);
      return billingProjectBufferEntryDao.save(entry);
    } finally {
      billingProjectBufferEntryDao.releaseAssigningLock();
    }
  }

  private int getUnfilledBufferSpace(DbAccessTier accessTier) {
    return getBufferMaxCapacity(accessTier.getShortName()) - (int) getCurrentBufferSize(accessTier);
  }

  private long getCurrentBufferSize(DbAccessTier accessTier) {
    return billingProjectBufferEntryDao.getCurrentBufferSizeForAccessTier(accessTier);
  }

  private int getBufferMaxCapacity(String accessTierShortName) {
    Map<String, Integer> capacityPerTier =
        workbenchConfigProvider.get().billing.bufferCapacityPerTier;

    if (!capacityPerTier.containsKey(accessTierShortName)) {
      final String message =
          String.format(
              "Workbench Config Error: no 'billing.bufferCapacityPerTier' entry for '%s' tier",
              accessTierShortName);
      log.severe(message);
      return 0;
    }

    return capacityPerTier.get(accessTierShortName);
  }

  public BillingProjectBufferStatus getStatus() {
    return new BillingProjectBufferStatus()
        .availablePerTier(
            billingProjectBufferEntryDao.getBillingBufferGaugeData().stream()
                .filter(tier -> tier.getStatusEnum() == BufferEntryStatus.AVAILABLE)
                .map(
                    tier ->
                        new AvailableBufferPerTier()
                            .accessTierShortName(tier.getAccessTier().getShortName())
                            .availableProjects(tier.getNumProjects()))
                .collect(Collectors.toList()));
  }
}
