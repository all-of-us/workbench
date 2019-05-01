package org.pmiops.workbench.billing;

import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.BillingProjectBufferEntryDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BillingProjectBufferService {

  private final BillingProjectBufferEntryDao billingProjectBufferEntryDao;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  @Autowired
  public BillingProjectBufferService(BillingProjectBufferEntryDao billingProjectBufferEntryDao,
      Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.billingProjectBufferEntryDao = billingProjectBufferEntryDao;
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  public void fillBuffer(int num) {
    getBufferCapacity();
    getCurrentBufferSize();
  }

  private long getCurrentBufferSize() {
    return billingProjectBufferEntryDao.count();
  }

  private int getBufferCapacity() {
    return 5;
  }
}
