package org.pmiops.workbench.cohorts;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.exceptions.NotFoundException;
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
    return StreamSupport.stream(cohortDao.findAllById(cohortIds).spliterator(), false)
        .map(cohortMapper::dbModelToClient)
        .collect(Collectors.toList());
  }

  public List<DbCohort> findAllByCohortIdIn(Collection<Long> cohortIds) {
    return cohortDao.findAllByCohortIdIn(cohortIds);
  }

  public Optional<DbCohort> findByCohortId(Long cohortId) {
    return cohortDao.findById(cohortId);
  }

  public DbCohort findByCohortIdOrThrow(Long cohortId) {
    return findByCohortId(cohortId)
        .orElseThrow(
            () ->
                new NotFoundException(
                    String.format("Cohort not found for cohortId: %d", cohortId)));
  }

  public List<Cohort> findByWorkspaceId(Long workspaceId) {
    return cohortDao.findByWorkspaceId(workspaceId).stream()
        .map(cohortMapper::dbModelToClient)
        .collect(Collectors.toList());
  }
}
