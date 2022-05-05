package org.pmiops.workbench.cohorts;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.Cohort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CohortService {

  private final BigQueryService bigQueryService;
  private final CohortQueryBuilder cohortQueryBuilder;
  private final CohortDao cohortDao;
  private final CohortMapper cohortMapper;

  @Autowired
  public CohortService(
      BigQueryService bigQueryService,
      CohortQueryBuilder cohortQueryBuilder,
      CohortDao cohortDao,
      CohortMapper cohortMapper) {
    this.bigQueryService = bigQueryService;
    this.cohortQueryBuilder = cohortQueryBuilder;
    this.cohortDao = cohortDao;
    this.cohortMapper = cohortMapper;
  }

  public List<Cohort> findAll(List<Long> cohortIds) {
    return StreamSupport.stream(cohortDao.findAllById(cohortIds).spliterator(), false)
        .map(cohortMapper::dbModelToClient)
        .collect(Collectors.toList());
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
