package org.pmiops.workbench.db.dao;

import java.sql.Timestamp;
import java.util.List;
import org.pmiops.workbench.db.model.DbEgressEvent;
import org.pmiops.workbench.db.model.DbEgressEvent.EgressEventStatus;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.springframework.data.repository.CrudRepository;

public interface EgressEventDao extends CrudRepository<DbEgressEvent, Long> {
  List<DbEgressEvent> findAllByUserAndStatusNot(DbUser user, EgressEventStatus status);

  List<DbEgressEvent> findAllByUserAndWorkspaceAndEgressWindowSecondsAndCreationTimeGreaterThan(
      DbUser user, DbWorkspace workspace, long egressWindowSeconds, Timestamp creationTimeLimit);

  List<DbEgressEvent> findAllByStatusAndLastModifiedTimeLessThan(
      EgressEventStatus status, Timestamp creationTimeLimit);
}
