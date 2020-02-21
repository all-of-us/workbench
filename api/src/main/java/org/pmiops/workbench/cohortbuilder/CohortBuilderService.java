package org.pmiops.workbench.cohortbuilder;

import org.pmiops.workbench.model.AgeType;

public interface CohortBuilderService {

  long countAgesByType(AgeType ageType, int startAge, int endAge);
}
