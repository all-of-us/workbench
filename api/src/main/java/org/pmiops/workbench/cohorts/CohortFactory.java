package org.pmiops.workbench.cohorts;

import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.User;

public interface CohortFactory {

    Cohort createCohort(org.pmiops.workbench.model.Cohort apiCohort, User creator, long workspaceId);

    Cohort duplicateCohort(org.pmiops.workbench.model.Cohort apiCohort);

}
