package org.pmiops.workbench.db.dao;

import java.util.List;
import org.pmiops.workbench.db.model.UserDataUseAgreement;
import org.springframework.data.repository.CrudRepository;

public interface UserDataUseAgreementDao extends CrudRepository<UserDataUseAgreement, Long> {
  List<UserDataUseAgreement> findByUserIdOrderByCompletionTimeDesc(long userId);
}
