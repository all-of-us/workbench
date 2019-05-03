package org.pmiops.workbench.billing;

import static org.pmiops.workbench.db.model.BillingProjectBufferEntry.Status.CREATING;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.UUID;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.BillingProjectBufferEntryDao;
import org.pmiops.workbench.db.model.BillingProjectBufferEntry;
import org.pmiops.workbench.db.model.BillingProjectBufferEntry.Status;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BillingProjectBufferService {

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

    String projectName = createBillingProjectName();

    fireCloudService.createAllOfUsBillingProject(projectName);
    BillingProjectBufferEntry entry = new BillingProjectBufferEntry();
    entry.setProjectName(projectName);
    entry.setCreationTime(new Timestamp(clock.instant().toEpochMilli()));
    entry.setStatus(CREATING);

    billingProjectBufferEntryDao.save(entry);
  }

  private String createBillingProjectName() {
    return workbenchConfigProvider.get().firecloud.billingProjectPrefix + "-" + UUID.randomUUID().toString();
  }

  private long getCurrentBufferSize() {
    return billingProjectBufferEntryDao.count();
  }

  private int getBufferCapacity() {
    return workbenchConfigProvider.get().firecloud.billingProjectBufferCapacity;
  }
}
