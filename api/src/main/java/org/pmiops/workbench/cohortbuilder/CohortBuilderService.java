package org.pmiops.workbench.cohortbuilder;

import java.util.List;
import org.pmiops.workbench.model.AgeTypeCount;

public interface CohortBuilderService {

  List<AgeTypeCount> findAgeTypeCounts(Long cdrVersionId);
}
