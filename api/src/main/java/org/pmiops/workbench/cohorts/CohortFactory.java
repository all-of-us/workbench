package org.pmiops.workbench.cohorts;

import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbCohortReview;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.db.model.DbUser;

public interface CohortFactory {

  DbCohort createCohort(org.pmiops.workbench.model.Cohort apiCohort, DbUser creator, long workspaceId);

  DbCohort duplicateCohort(String newName, DbUser creator, DbWorkspace targetWorkspace, DbCohort original);

  DbCohortReview duplicateCohortReview(DbCohortReview original, DbCohort targetCohort);
}
