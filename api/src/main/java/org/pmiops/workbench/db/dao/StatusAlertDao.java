package org.pmiops.workbench.db.dao;

import java.util.List;
import org.pmiops.workbench.db.model.DbStatusAlert;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StatusAlertDao extends CrudRepository<DbStatusAlert, Long> {
  List<DbStatusAlert> findAll();
}
