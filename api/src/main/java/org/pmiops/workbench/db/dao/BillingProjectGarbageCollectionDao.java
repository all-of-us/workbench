package org.pmiops.workbench.db.dao;

import java.util.List;
import org.pmiops.workbench.db.model.BillingProjectGarbageCollection;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BillingProjectGarbageCollectionDao
    extends CrudRepository<BillingProjectGarbageCollection, Long> {

  // get Billing Projects which are ASSIGNED to Workspaces which have been DELETED and have NEW
  // Billing Migration Status.  These are ready to be garbage-collected
  @Query(
      "SELECT p.fireCloudProjectName "
          + "FROM BillingProjectBufferEntry p "
          + "JOIN Workspace w "
          + "ON w.workspaceNamespace = p.fireCloudProjectName "
          + "AND p.status = 4 " // BillingProjectBufferStatus.ASSIGNED
          + "AND w.activeStatus = 1 " // WorkspaceActiveStatus.DELETED
          + "AND w.billingMigrationStatus = 1") // BillingMigrationStatus.NEW
  List<String> findBillingProjectsForGarbageCollection();

  Long countAllByOwner(String owner);
}
