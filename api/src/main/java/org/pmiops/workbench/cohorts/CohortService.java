package org.pmiops.workbench.cohorts;

import java.util.List;
import java.util.stream.Collectors;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.model.Cohort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CohortService {
  private final CohortDao cohortDao;
  private final CohortMapper cohortMapper;

  @Autowired
  public CohortService(CohortDao cohortDao, CohortMapper cohortMapper) {
    this.cohortDao = cohortDao;
    this.cohortMapper = cohortMapper;
  }

  public List<Cohort> findAll(List<Long> cohortIds) {
    return ((List<DbCohort>) cohortDao.findAll(cohortIds))
        .stream().map(cohortMapper::dbModelToClient).collect(Collectors.toList());
  }
}
