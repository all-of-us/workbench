package org.pmiops.workbench.cohorts;

import org.pmiops.workbench.db.model.CohortDataModel;
import org.pmiops.workbench.db.model.CohortReview;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.db.model.User;

public interface CohortFactory {

  CohortDataModel createCohort(org.pmiops.workbench.model.Cohort apiCohort, User creator, long workspaceId);

  CohortDataModel duplicateCohort(
      String newName, User creator, DbWorkspace targetWorkspace, CohortDataModel original);

  CohortReview duplicateCohortReview(CohortReview original, CohortDataModel targetCohort);
}
