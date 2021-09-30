package org.pmiops.workbench.db.dao;

import java.sql.Timestamp;
import java.util.List;
import org.pmiops.workbench.db.model.DbEgressEvent;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.springframework.data.repository.CrudRepository;

public interface EgressEventDao extends CrudRepository<DbEgressEvent, Long> {

  List<DbEgressEvent> findAllByUserAndWorkspaceAndCreationTimeGreaterThan(
      DbUser user, DbWorkspace workspace, Timestamp creationTimeLimit);
}
