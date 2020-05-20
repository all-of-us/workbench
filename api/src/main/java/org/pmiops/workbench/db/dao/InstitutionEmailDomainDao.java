package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.DbInstitutionEmailDomain;
import org.springframework.data.repository.CrudRepository;

public interface InstitutionEmailDomainDao extends CrudRepository<DbInstitutionEmailDomain, Long> {
  void deleteDbInstitutionEmailDomainByInstitution_ShortName(String shortNAME);
}
