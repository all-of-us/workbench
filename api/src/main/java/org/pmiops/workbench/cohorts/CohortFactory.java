package org.pmiops.workbench.cohorts;

import org.pmiops.workbench.db.model.Cohort;

public interface CohortFactory {

    Cohort createCohort(org.pmiops.workbench.model.Cohort apiCohort);

    Cohort duplicateCohort(org.pmiops.workbench.model.Cohort apiCohort);

}
