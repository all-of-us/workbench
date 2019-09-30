package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.BillingProjectGarbageCollection;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BillingProjectGarbageCollectionDao
    extends CrudRepository<BillingProjectGarbageCollection, Long> {
  Long countAllByOwner(String owner);
}
