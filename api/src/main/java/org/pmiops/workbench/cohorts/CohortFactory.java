package org.pmiops.workbench.cohorts;

import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.User;

public interface CohortFactory {

    Cohort createCohort(org.pmiops.workbench.model.Cohort apiCohort, User creator, long workspaceId);

    // duplicateCohort provides the functionality for the "Duplicate" Cohort function in the AoU workspace.
    // It is NOT a deep cloning method so some fields (CohortReviews) will not be copied over
    Cohort duplicateCohort(String newName, Cohort original, User creator);

}
