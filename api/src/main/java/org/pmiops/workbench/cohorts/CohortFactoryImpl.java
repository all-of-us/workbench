package org.pmiops.workbench.cohorts;

import org.pmiops.workbench.db.model.Cohort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Clock;

@Service
public class CohortFactoryImpl implements CohortFactory {

    private final Clock clock;

    @Autowired
    public CohortFactoryImpl(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Cohort createCohort(org.pmiops.workbench.model.Cohort apiCohort) {
        Timestamp now = new Timestamp(clock.instant().toEpochMilli());

        Cohort dbCohort = new org.pmiops.workbench.db.model.Cohort();
        dbCohort.setCriteria(apiCohort.getCriteria());
        dbCohort.setDescription(apiCohort.getDescription());
        dbCohort.setName(apiCohort.getName());
        dbCohort.setType(apiCohort.getType());
        dbCohort.setCreationTime(now);
        dbCohort.setLastModifiedTime(now);
        dbCohort.setVersion(1);

        return dbCohort;
    }

    @Override
    public Cohort duplicateCohort(org.pmiops.workbench.model.Cohort apiCohort) {
        return null;
    }

}
