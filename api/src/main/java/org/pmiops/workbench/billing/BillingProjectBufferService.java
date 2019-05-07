package org.pmiops.workbench.billing;

import static org.pmiops.workbench.db.model.BillingProjectBufferEntry.BillingProjectBufferStatus.AVAILABLE;
import static org.pmiops.workbench.db.model.BillingProjectBufferEntry.BillingProjectBufferStatus.CREATING;
import static org.pmiops.workbench.db.model.BillingProjectBufferEntry.BillingProjectBufferStatus.ERROR;

import com.google.common.hash.Hashing;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.UUID;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.BillingProjectBufferEntryDao;
import org.pmiops.workbench.db.model.BillingProjectBufferEntry;
import org.pmiops.workbench.db.model.StorageEnums;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BillingProjectBufferService {

  private static final int PROJECT_BILLING_ID_SIZE = 8;
  private static final Logger log = Logger.getLogger(BillingProjectBufferService.class.getName());

  private final BillingProjectBufferEntryDao billingProjectBufferEntryDao;
  private final Clock clock;
  private final FireCloudService fireCloudService;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  @Autowired
  public BillingProjectBufferService(BillingProjectBufferEntryDao billingProjectBufferEntryDao,
      Clock clock,
      FireCloudService fireCloudService,
      Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.billingProjectBufferEntryDao = billingProjectBufferEntryDao;
    this.clock = clock;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.fireCloudService = fireCloudService;
  }

  public void bufferBillingProject() {
    if (getCurrentBufferSize() >= getBufferCapacity()) {
      return;
    }

    final String projectName = createBillingProjectName();

    fireCloudService.createAllOfUsBillingProject(projectName);
    BillingProjectBufferEntry entry = new BillingProjectBufferEntry();
    entry.setFireCloudProjectName(projectName);
    entry.setCreationTime(new Timestamp(clock.instant().toEpochMilli()));
    entry.setStatusEnum(CREATING);

    billingProjectBufferEntryDao.save(entry);
  }

  public void syncBillingProjectStatus() {
    BillingProjectBufferEntry entry = billingProjectBufferEntryDao
        .findFirstByStatusOrderByCreationTimeAsc(StorageEnums.billingProjectBufferStatusToStorage(CREATING));

    if (entry == null) {
      return;
    }

    switch (fireCloudService.getBillingProjectStatus(entry.getFireCloudProjectName()).getCreationStatus()) {
      case READY:
        entry.setStatusEnum(AVAILABLE);
        break;
      case ERROR:
        log.warning(String.format("SyncBillingProjectStatus: BillingProject %s creation failed", entry.getFireCloudProjectName()));
        entry.setStatusEnum(ERROR);
        break;
    }

    billingProjectBufferEntryDao.save(entry);
  }

  private String createBillingProjectName() {
    String randomString = Hashing.sha256().hashUnencodedChars(UUID.randomUUID().toString()).toString()
        .substring(0, PROJECT_BILLING_ID_SIZE);

    String prefix = workbenchConfigProvider.get().firecloud.billingProjectPrefix;
    if (!prefix.endsWith("-")) {
      prefix = prefix + "-";
    }

    return prefix + randomString;
  }

  private long getCurrentBufferSize() {
    return billingProjectBufferEntryDao.getCurrentBufferSize();
  }

  private int getBufferCapacity() {
    return workbenchConfigProvider.get().firecloud.billingProjectBufferCapacity;
  }

}
