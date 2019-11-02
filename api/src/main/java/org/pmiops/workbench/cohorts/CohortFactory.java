package org.pmiops.workbench.cohorts;

import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.CohortReview;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.db.model.User;

public interface CohortFactory {

  DbCohort createCohort(org.pmiops.workbench.model.Cohort apiCohort, User creator, long workspaceId);

  DbCohort duplicateCohort(String newName, User creator, DbWorkspace targetWorkspace, DbCohort original);

  CohortReview duplicateCohortReview(CohortReview original, DbCohort targetCohort);
}
