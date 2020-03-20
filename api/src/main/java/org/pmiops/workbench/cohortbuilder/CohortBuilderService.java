package org.pmiops.workbench.cohortbuilder;

import java.util.List;
import org.pmiops.workbench.model.AgeTypeCount;
import org.pmiops.workbench.model.DataFilter;

public interface CohortBuilderService {

  List<AgeTypeCount> findAgeTypeCounts(Long cdrVersionId);

  List<DataFilter> findDataFilters(Long cdrVersionId);
}
