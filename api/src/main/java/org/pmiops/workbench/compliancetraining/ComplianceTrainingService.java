package org.pmiops.workbench.compliancetraining;

import org.pmiops.workbench.absorb.ApiException;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.NotFoundException;

public interface ComplianceTrainingService {
  public DbUser syncComplianceTrainingStatus() throws NotFoundException, ApiException;
}
