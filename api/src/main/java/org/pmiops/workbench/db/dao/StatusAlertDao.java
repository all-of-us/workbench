package org.pmiops.workbench.db.dao;

import java.util.Optional;
import org.pmiops.workbench.db.model.DbStatusAlert;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StatusAlertDao extends CrudRepository<DbStatusAlert, Long> {
  // First By is needed so that JPA ever looks for the second By.
  Optional<DbStatusAlert> findFirstByOrderByStatusAlertIdDesc();
}
