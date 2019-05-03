package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.BillingProjectBufferEntry;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BillingProjectBufferEntryDao  extends CrudRepository<BillingProjectBufferEntry, Long> {

  BillingProjectBufferEntry findByProjectName(String projectName);

  @Query("SELECT COUNT(*) FROM BillingProjectBufferEntry WHERE status=0 OR status=2 OR status=3")
  Long getCurrentBufferSize();

}
