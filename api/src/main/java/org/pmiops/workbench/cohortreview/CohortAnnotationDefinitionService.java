package org.pmiops.workbench.cohortreview;

import java.util.List;
import org.pmiops.workbench.model.CohortAnnotationDefinition;

public interface CohortAnnotationDefinitionService {

  boolean definitionExists(Long cohortId, String columnName);

  void delete(Long annotationDefinitionId);

  List<CohortAnnotationDefinition> findByCohortId(Long cohortId);

  CohortAnnotationDefinition findByCohortIdAndCohortAnnotationDefinitionId(
      Long cohortId, Long annotationDefinitionId);

  CohortAnnotationDefinition save(CohortAnnotationDefinition cohortAnnotationDefinition);
}
