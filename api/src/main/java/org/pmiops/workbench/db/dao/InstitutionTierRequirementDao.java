package org.pmiops.workbench.db.dao;

import java.util.Set;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbInstitutionTierRequirement;
import org.springframework.data.repository.CrudRepository;

public interface InstitutionTierRequirementDao extends CrudRepository<DbInstitutionTierRequirement, Long> {

  Set<DbInstitutionTierRequirement> getByInstitution(final DbInstitution institution);
}
