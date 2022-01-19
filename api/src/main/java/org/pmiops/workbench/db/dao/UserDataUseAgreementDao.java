package org.pmiops.workbench.db.dao;

import java.util.List;
import org.pmiops.workbench.db.model.DbUserDataUseAgreement;
import org.springframework.data.repository.CrudRepository;

@Deprecated() // will be replaced by UserCodeOfConductAgreementDao as part of RW-4838
public interface UserDataUseAgreementDao extends CrudRepository<DbUserDataUseAgreement, Long> {
  List<DbUserDataUseAgreement> findByUserIdOrderByCompletionTimeDesc(long userId);
}
