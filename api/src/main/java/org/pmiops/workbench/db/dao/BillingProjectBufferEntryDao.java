package org.pmiops.workbench.db.dao;

import java.sql.Timestamp;
import java.util.List;
import org.pmiops.workbench.db.model.BillingProjectBufferEntry;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BillingProjectBufferEntryDao
    extends CrudRepository<BillingProjectBufferEntry, Long> {

  String ASSIGNING_LOCK = "ASSIGNING_LOCK";

  BillingProjectBufferEntry findByFireCloudProjectName(String fireCloudProjectName);

  @Query("SELECT COUNT(*) FROM BillingProjectBufferEntry WHERE status IN (0, 2)")
  Long getCurrentBufferSize();

  List<BillingProjectBufferEntry> findAllByStatusAndLastStatusChangedTimeLessThan(short status, Timestamp timestamp);

  BillingProjectBufferEntry findFirstByStatusOrderByLastSyncRequestTimeAsc(short status);

  BillingProjectBufferEntry findFirstByStatusOrderByCreationTimeAsc(short status);

  Long countByStatus(short status);

  @Query(value = "SELECT GET_LOCK('" + ASSIGNING_LOCK + "', 1)", nativeQuery = true)
  int acquireAssigningLock();

  @Query(value = "SELECT RELEASE_LOCK('" + ASSIGNING_LOCK + "')", nativeQuery = true)
  int releaseAssigningLock();
}
