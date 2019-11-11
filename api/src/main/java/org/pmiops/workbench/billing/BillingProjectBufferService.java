package org.pmiops.workbench.billing;

import com.google.common.collect.Iterables;
import com.google.common.hash.Hashing;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import org.pmiops.workbench.model.BillingProjectBufferStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BillingProjectBufferService {

  private static final Logger log = Logger.getLogger(BillingProjectBufferService.class.getName());

  private static final int SYNCS_PER_INVOCATION = 5;
  private static final int PROJECT_BILLING_ID_SIZE = 8;
  private static final int CREATING_TIMEOUT_MINUTES = 60;
  private static final int ASSIGNING_TIMEOUT_MINUTES = 10;

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
      return;
    }

    final String projectName = createBillingProjectName();

    DbBillingProjectBufferEntry entry = new DbBillingProjectBufferEntry();
    entry.setFireCloudProjectName(projectName);
    entry.setCreationTime(new Timestamp(clock.instant().toEpochMilli()));
    entry.setStatusEnum(BufferEntryStatus.CREATING, this::getCurrentTimestamp);
    billingProjectBufferEntryDao.save(entry);

    fireCloudService.createAllOfUsBillingProject(projectName);
  }

  public void syncBillingProjectStatus() {
    for (int i = 0; i < SYNCS_PER_INVOCATION; i++) {
      DbBillingProjectBufferEntry entry =
          billingProjectBufferEntryDao.findFirstByStatusOrderByLastSyncRequestTimeAsc(
              DbStorageEnums.billingProjectBufferEntryStatusToStorage(BufferEntryStatus.CREATING));

      if (entry == null) {
        return;
      }

      entry.setLastSyncRequestTime(new Timestamp(clock.instant().toEpochMilli()));

      try {
        switch (fireCloudService
            .getBillingProjectStatus(entry.getFireCloudProjectName())
            .getCreationStatus()) {
          case READY:
            entry.setStatusEnum(BufferEntryStatus.AVAILABLE, this::getCurrentTimestamp);
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
            break;
        }
      } catch (NotFoundException e) {
        log.log(
            Level.WARNING,
            "Get BillingProjectStatus call failed for "
                + entry.getFireCloudProjectName()
                + ". Project not found.",
            e);
      } catch (WorkbenchException e) {
        log.log(
            Level.WARNING,
            "Get BillingProjectStatus call failed for " + entry.getFireCloudProjectName(),
            e);
      }

      billingProjectBufferEntryDao.save(entry);
    }
  }

  public void cleanBillingBuffer() {
    Iterables.concat(
            billingProjectBufferEntryDao.findAllByStatusAndLastStatusChangedTimeLessThan(
                DbStorageEnums.billingProjectBufferEntryStatusToStorage(BufferEntryStatus.CREATING),
                new Timestamp(
                    clock
                        .instant()
                        .minus(CREATING_TIMEOUT_MINUTES, ChronoUnit.MINUTES)
                        .toEpochMilli())),
            billingProjectBufferEntryDao.findAllByStatusAndLastStatusChangedTimeLessThan(
                DbStorageEnums.billingProjectBufferEntryStatusToStorage(
                    BufferEntryStatus.ASSIGNING),
                new Timestamp(
                    clock
                        .instant()
                        .minus(ASSIGNING_TIMEOUT_MINUTES, ChronoUnit.MINUTES)
                        .toEpochMilli())))
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

  public DbBillingProjectBufferEntry assignBillingProject(DbUser user) {
    DbBillingProjectBufferEntry entry = consumeBufferEntryForAssignment();

    fireCloudService.addUserToBillingProject(user.getEmail(), entry.getFireCloudProjectName());
    entry.setStatusEnum(BufferEntryStatus.ASSIGNED, this::getCurrentTimestamp);
    entry.setAssignedUser(user);
    billingProjectBufferEntryDao.save(entry);

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
    final BillingProjectBufferStatus result = new BillingProjectBufferStatus();
    result.setBufferSize(bufferSize);
    return result;
  }
}
