package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.DbInstitutionEmailAddress;
import org.springframework.data.repository.CrudRepository;

public interface InstitutionEmailAddressDao
    extends CrudRepository<DbInstitutionEmailAddress, Long> {
  void deleteDbInstitutionEmailAddressesByInstitution_ShortName(String shortNAME);
}
