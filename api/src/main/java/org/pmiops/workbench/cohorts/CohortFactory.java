package org.pmiops.workbench.cohorts;

import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.CohortReview;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.db.model.User;

public interface CohortFactory {

  Cohort createCohort(org.pmiops.workbench.model.Cohort apiCohort, User creator, long workspaceId);

  Cohort duplicateCohort(String newName, User creator, DbWorkspace targetWorkspace, Cohort original);

  CohortReview duplicateCohortReview(CohortReview original, Cohort targetCohort);
}
