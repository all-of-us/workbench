package org.pmiops.workbench.db.dao;

import java.sql.Timestamp;
import java.util.List;
import org.pmiops.workbench.db.model.DbEgressEvent;
import org.pmiops.workbench.db.model.DbEgressEvent.DbEgressEventStatus;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface EgressEventDao extends PagingAndSortingRepository<DbEgressEvent, Long> {
  Page<DbEgressEvent> findAllOrderByCreationTimeDesc(Pageable p);

  Page<DbEgressEvent> findAllByUserOrderByCreationTimeDesc(DbUser user, Pageable p);

  Page<DbEgressEvent> findAllByUser(DbUser user, Pageable p);

  Page<DbEgressEvent> findAllByWorkspaceOrderByCreationTimeDesc(DbWorkspace workspace, Pageable p);

  Page<DbEgressEvent> findAllByUserAndWorkspaceOrderByCreationTimeDesc(
      DbUser user, DbWorkspace workspace, Pageable p);

  List<DbEgressEvent> findAllByUserAndStatusNot(DbUser user, DbEgressEventStatus status);

  List<DbEgressEvent> findAllByUserAndWorkspaceAndCreationTimeBetweenAndCreationTimeNot(
      DbUser user,
      DbWorkspace workspace,
      Timestamp createdAfter,
      Timestamp createdBefore,
      Timestamp notCreatedAt);

  List<DbEgressEvent> findAllByUserAndWorkspaceAndEgressWindowSecondsAndCreationTimeGreaterThan(
      DbUser user, DbWorkspace workspace, long egressWindowSeconds, Timestamp creationTimeLimit);

  List<DbEgressEvent> findAllByStatusAndLastModifiedTimeLessThan(
      DbEgressEventStatus status, Timestamp creationTimeLimit);
}
