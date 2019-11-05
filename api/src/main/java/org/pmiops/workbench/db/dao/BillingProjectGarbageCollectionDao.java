package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.DbBillingProjectGarbageCollection;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BillingProjectGarbageCollectionDao
    extends CrudRepository<DbBillingProjectGarbageCollection, Long> {
  Long countAllByOwner(String owner);
}
