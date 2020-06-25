package org.pmiops.workbench.cohorts;

import java.util.List;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.model.DbCohort;
import org.springframework.stereotype.Service;

@Service
public class CohortService {
  private CohortDao cohortDao;

  public CohortService(CohortDao cohortDao) {
    this.cohortDao = cohortDao;
  }

  public List<DbCohort> findAll(List<Long> cohortIds) {
    return (List<DbCohort>) cohortDao.findAll(cohortIds);
  }
}
