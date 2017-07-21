package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.Cohort;
import org.springframework.data.repository.CrudRepository;

public interface CohortDao extends CrudRepository<Cohort, Long> {

}
