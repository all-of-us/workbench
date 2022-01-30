package org.pmiops.workbench.db.dao;

import java.util.List;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserCodeOfConductAgreement;
import org.springframework.data.repository.CrudRepository;

public interface UserCodeOfConductAgreementDao
    extends CrudRepository<DbUserCodeOfConductAgreement, Long> {
  List<DbUserCodeOfConductAgreement> findByUserOrderByCompletionTimeDesc(DbUser user);
}
