package org.pmiops.workbench.db.dao;

import java.util.Set;
import org.pmiops.workbench.db.model.DbInstitutionEmailDomain;
import org.springframework.data.repository.CrudRepository;

public interface InstitutionEmailDomainDao extends CrudRepository<DbInstitutionEmailDomain, Long> {

  Set<DbInstitutionEmailDomain> getByInstitutionId(final long institutionId);

  long deleteByInstitutionId(final long institutionId);
}
