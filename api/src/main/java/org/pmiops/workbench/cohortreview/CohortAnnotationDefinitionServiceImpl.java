package org.pmiops.workbench.cohortreview;

import java.util.List;
import java.util.stream.Collectors;
import org.pmiops.workbench.cohortreview.mappers.CohortAnnotationDefinitionMapper;
import org.pmiops.workbench.db.dao.CohortAnnotationDefinitionDao;
import org.pmiops.workbench.db.model.DbCohortAnnotationDefinition;
import org.pmiops.workbench.db.model.DbCohortAnnotationEnumValue;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.CohortAnnotationDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CohortAnnotationDefinitionServiceImpl implements CohortAnnotationDefinitionService {

  private CohortAnnotationDefinitionDao cohortAnnotationDefinitionDao;
  private CohortAnnotationDefinitionMapper cohortAnnotationDefinitionMapper;

  @Autowired
  public CohortAnnotationDefinitionServiceImpl(
      CohortAnnotationDefinitionDao cohortAnnotationDefinitionDao,
      CohortAnnotationDefinitionMapper cohortAnnotationDefinitionMapper) {
    this.cohortAnnotationDefinitionDao = cohortAnnotationDefinitionDao;
    this.cohortAnnotationDefinitionMapper = cohortAnnotationDefinitionMapper;
  }

  @Override
  public boolean definitionExists(Long cohortId, String columnName) {
    return cohortAnnotationDefinitionDao.findByCohortIdAndColumnName(cohortId, columnName) != null;
  }

  @Override
  public void delete(Long annotationDefinitionId) {
    cohortAnnotationDefinitionDao.delete(annotationDefinitionId);
  }

  @Override
  public List<CohortAnnotationDefinition> findByCohortId(Long cohortId) {
    return cohortAnnotationDefinitionDao.findByCohortId(cohortId).stream()
        .map(cohortAnnotationDefinitionMapper::dbModelToClient)
        .collect(Collectors.toList());
  }

  @Override
  public CohortAnnotationDefinition findByCohortIdAndCohortAnnotationDefinitionId(
      Long cohortId, Long annotationDefinitionId) {
    DbCohortAnnotationDefinition dbCohortAnnotationDefinition =
        cohortAnnotationDefinitionDao.findByCohortIdAndCohortAnnotationDefinitionId(
            cohortId, annotationDefinitionId);
    if (dbCohortAnnotationDefinition == null) {
      throw new NotFoundException(
          String.format(
              "Not Found: No Cohort Annotation Definition exists for annotationDefinitionId: %s",
              annotationDefinitionId));
    }
    return cohortAnnotationDefinitionMapper.dbModelToClient(dbCohortAnnotationDefinition);
  }

  @Override
  public CohortAnnotationDefinition save(CohortAnnotationDefinition cohortAnnotationDefinition) {
    DbCohortAnnotationDefinition dbCohortAnnotationDefinition =
        cohortAnnotationDefinitionMapper.clientToDbModel(cohortAnnotationDefinition);
    for (DbCohortAnnotationEnumValue enumValue : dbCohortAnnotationDefinition.getEnumValues()) {
      enumValue.setCohortAnnotationDefinition(dbCohortAnnotationDefinition);
    }
    dbCohortAnnotationDefinition = cohortAnnotationDefinitionDao.save(dbCohortAnnotationDefinition);
    return cohortAnnotationDefinitionMapper.dbModelToClient(dbCohortAnnotationDefinition);
  }
}
