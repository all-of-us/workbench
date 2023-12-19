package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.DbGoogleProjectPerCost;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GoogleProjectPerCostRepository
    extends CrudRepository<DbGoogleProjectPerCost, String> {}
