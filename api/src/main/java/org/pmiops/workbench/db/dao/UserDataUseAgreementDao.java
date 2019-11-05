package org.pmiops.workbench.db.dao;

import java.util.List;
import org.pmiops.workbench.db.model.DbUserDataUseAgreement;
import org.springframework.data.repository.CrudRepository;

public interface UserDataUseAgreementDao extends CrudRepository<DbUserDataUseAgreement, Long> {
  List<DbUserDataUseAgreement> findByUserIdOrderByCompletionTimeDesc(long userId);
}
