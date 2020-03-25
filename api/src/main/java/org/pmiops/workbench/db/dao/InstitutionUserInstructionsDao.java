package org.pmiops.workbench.db.dao;

import java.util.Optional;
import org.pmiops.workbench.db.model.DbInstitutionUserInstructions;
import org.springframework.data.repository.CrudRepository;

public interface InstitutionUserInstructionsDao
    extends CrudRepository<DbInstitutionUserInstructions, Long> {

  Optional<DbInstitutionUserInstructions> getByInstitutionId(final long institutionId);

  long deleteByInstitutionId(final long institutionId);
}
