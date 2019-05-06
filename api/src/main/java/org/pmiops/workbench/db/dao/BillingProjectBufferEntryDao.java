package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.BillingProjectBufferEntry;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BillingProjectBufferEntryDao  extends CrudRepository<BillingProjectBufferEntry, Long> {

  BillingProjectBufferEntry findByFireCloudProjectName(String fireCloudProjectName);

  @Query("SELECT COUNT(*) FROM BillingProjectBufferEntry WHERE status IN (0, 2)")
  Long getCurrentBufferSize();

}
