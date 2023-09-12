package org.pmiops.workbench.db.dao;

import java.util.Optional;
import org.pmiops.workbench.db.model.DbComplianceTrainingVerification;
import org.pmiops.workbench.db.model.DbUserAccessModule;
import org.springframework.data.repository.CrudRepository;

public interface ComplianceTrainingVerificationDao
    extends CrudRepository<DbComplianceTrainingVerification, Long> {
  Optional<DbComplianceTrainingVerification> getByUserAccessModule(DbUserAccessModule uam);
}
