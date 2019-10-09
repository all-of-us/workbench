package org.pmiops.workbench.billing;

import static org.pmiops.workbench.db.model.BillingProjectBufferEntry.BillingProjectBufferStatus.ASSIGNED;
import static org.pmiops.workbench.db.model.BillingProjectBufferEntry.BillingProjectBufferStatus.ASSIGNING;
import static org.pmiops.workbench.db.model.BillingProjectBufferEntry.BillingProjectBufferStatus.AVAILABLE;
import static org.pmiops.workbench.db.model.BillingProjectBufferEntry.BillingProjectBufferStatus.CREATING;
import static org.pmiops.workbench.db.model.BillingProjectBufferEntry.BillingProjectBufferStatus.ERROR;

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
import org.pmiops.workbench.db.model.BillingProjectBufferEntry;
import org.pmiops.workbench.db.model.StorageEnums;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.firecloud.FireCloudService;
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

  public void bufferBillingProject() {
    if (getUnfilledBufferSpace() <= 0) {
      return;
    }

    final String projectName = createBillingProjectName();

    BillingProjectBufferEntry entry = new BillingProjectBufferEntry();
    entry.setFireCloudProjectName(projectName);
    entry.setCreationTime(new Timestamp(clock.instant().toEpochMilli()));
    entry.setStatusEnum(CREATING, this::getCurrentTimestamp);
    billingProjectBufferEntryDao.save(entry);

    fireCloudService.createAllOfUsBillingProject(projectName);
  }

  public void syncBillingProjectStatus() {
    for (int i = 0; i < SYNCS_PER_INVOCATION; i++) {
      BillingProjectBufferEntry entry =
          billingProjectBufferEntryDao.findFirstByStatusOrderByLastSyncRequestTimeAsc(
              StorageEnums.billingProjectBufferStatusToStorage(CREATING));

      if (entry == null) {
        return;
      }

      entry.setLastSyncRequestTime(new Timestamp(clock.instant().toEpochMilli()));

      try {
        switch (fireCloudService
            .getBillingProjectStatus(entry.getFireCloudProjectName())
            .getCreationStatus()) {
          case READY:
            entry.setStatusEnum(AVAILABLE, this::getCurrentTimestamp);
            break;
          case ERROR:
            log.warning(
                String.format(
                    "SyncBillingProjectStatus: BillingProject %s creation failed",
                    entry.getFireCloudProjectName()));
            entry.setStatusEnum(ERROR, this::getCurrentTimestamp);
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
                StorageEnums.billingProjectBufferStatusToStorage(CREATING),
                new Timestamp(
                    clock
                        .instant()
                        .minus(CREATING_TIMEOUT_MINUTES, ChronoUnit.MINUTES)
                        .toEpochMilli())),
            billingProjectBufferEntryDao.findAllByStatusAndLastStatusChangedTimeLessThan(
                StorageEnums.billingProjectBufferStatusToStorage(ASSIGNING),
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
              entry.setStatusEnum(ERROR, this::getCurrentTimestamp);
              billingProjectBufferEntryDao.save(entry);
            });
  }

  public BillingProjectBufferEntry assignBillingProject(User user) {
    BillingProjectBufferEntry entry = consumeBufferEntryForAssignment();

    fireCloudService.addUserToBillingProject(user.getEmail(), entry.getFireCloudProjectName());
    entry.setStatusEnum(ASSIGNED, this::getCurrentTimestamp);
    entry.setAssignedUser(user);
    billingProjectBufferEntryDao.save(entry);

    return entry;
  }

  private BillingProjectBufferEntry consumeBufferEntryForAssignment() {
    // Each call to acquire the lock will timeout in 1s if it is currently held
    while (billingProjectBufferEntryDao.acquireAssigningLock() != 1) {}

    BillingProjectBufferEntry entry;
    try {
      entry =
          billingProjectBufferEntryDao.findFirstByStatusOrderByCreationTimeAsc(
              StorageEnums.billingProjectBufferStatusToStorage(AVAILABLE));

      if (entry == null) {
        log.log(Level.SEVERE, "Consume Buffer call made while Billing Project Buffer was empty");
        throw new EmptyBufferException();
      }

      entry.setStatusEnum(ASSIGNING, this::getCurrentTimestamp);
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
  
}
