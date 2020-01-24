package org.pmiops.workbench.db.dao;

import java.util.List;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbInstitutionEmailAddress;
import org.springframework.data.repository.CrudRepository;

public interface InstitutionEmailAddressDao
    extends CrudRepository<DbInstitutionEmailAddress, Long> {

  List<DbInstitutionEmailAddress> findAllByInstitution(DbInstitution institution);

  default void deleteAllByInstitution(DbInstitution institution) {
    delete(findAllByInstitution(institution));
  }
}
