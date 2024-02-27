package org.pmiops.workbench.compliancetraining;

import org.pmiops.workbench.absorb.ApiException;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.NotFoundException;

public interface ComplianceTrainingService {
  DbUser syncComplianceTrainingStatus()
      throws org.pmiops.workbench.moodle.ApiException, NotFoundException, ApiException;
}
